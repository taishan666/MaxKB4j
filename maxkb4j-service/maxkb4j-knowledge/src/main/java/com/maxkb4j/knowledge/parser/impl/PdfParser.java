package com.maxkb4j.knowledge.parser.impl;

import com.benjaminwan.ocrlibrary.OcrResult;
import com.maxkb4j.knowledge.parser.DocumentParser;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class PdfParser extends PDFTextStripper implements DocumentParser {

    private List<TextLine> lines;
    private final static PDFTextStripper stripper = new PDFTextStripper();
    private final static InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);

    @Override
    public List<String> getExtensions() {
        return List.of(".pdf");
    }

    @Override
    protected void writePage() throws IOException {
        super.writePage();
        lines.add(new TextLine("unknown", "\n", 0));
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        if (text == null || text.isEmpty() || textPositions == null || textPositions.isEmpty()) {
            return;
        }
        TextPosition first = textPositions.get(0);
        String fontName = getFontName(first.getFont());
        float fontSize = first.getFontSizeInPt();
        lines.add(new TextLine(fontName, text, fontSize));
    }

    private String getFontName(PDFont font) {
        if (font == null) {
            return "unknown";
        }
        try {
            return font.getName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public String handle(InputStream inputStream) {
        this.lines = new ArrayList<>();
        try {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (isScannedPDF(document)) {
                    return extractTextFromScannedPDF(document);
                }
                this.setSortByPosition(true);
                this.setStartPage(1);
                this.setEndPage(document.getNumberOfPages());
                this.getText(document);
                List<TextLine> mergedLines = mergeConsecutiveLines(lines);
                Map<Float, Integer> fontSizeToHeadingLevel = buildFontSizeHeadingMap(mergedLines);
                return toMarkdown(mergedLines, fontSizeToHeadingLevel);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PDF from input stream", e);
        }
    }

    private static boolean isScannedPDF(PDDocument document) throws IOException {
        String text = stripper.getText(document);
        String cleanText = text.replaceAll("\\s+", "");
        return cleanText.length() < 10;
    }

    private static String extractTextFromScannedPDF(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder fullText = new StringBuilder();
        int totalPages = document.getNumberOfPages();
        for (int i = 0; i < totalPages; i++) {
            BufferedImage image = renderer.renderImage(i);
            Path tempFile = Files.createTempFile("pdf_page_", ".png");
            try {
                ImageIO.write(image, "png", tempFile.toFile());
                OcrResult result = engine.runOcr(tempFile.toString());
                fullText.append(result.getStrRes()).append("\n\n");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
        return fullText.toString();
    }

    /**
     * 统计各字号出现次数，按出现次数降序排列，
     * 出现最多的字号视为正文基准，比基准大的字号按大小映射为标题层级
     */
    private static Map<Float, Integer> buildFontSizeHeadingMap(List<TextLine> lines) {
        // 统计非换行、非零字号的频率
        Map<Float, Integer> freq = new LinkedHashMap<>();
        for (TextLine line : lines) {
            if (line.fontSize() > 0 && !line.text().equals("\n")) {
                freq.merge(line.fontSize(), 1, Integer::sum);
            }
        }
        if (freq.isEmpty()) {
            return Map.of();
        }
        // 找到出现次数最多的字号作为正文基准
        float bodyFontSize = freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(12f);

        // 比基准字号大的按从大到小排序，映射为 h1 ~ h6
        List<Float> headingSizes = freq.keySet().stream()
                .filter(s -> s > bodyFontSize)
                .sorted(Comparator.reverseOrder())
                .toList();

        Map<Float, Integer> sizeToLevel = new LinkedHashMap<>();
        for (int i = 0; i < headingSizes.size() && i < 6; i++) {
            sizeToLevel.put(headingSizes.get(i), i + 1); // h1=1, h2=2, ...
        }
        // 基准字号及更小的字号 → 段落（level=0）
        sizeToLevel.put(bodyFontSize, 0);
        freq.keySet().stream()
                .filter(s -> s < bodyFontSize && s > 0)
                .forEach(s -> sizeToLevel.put(s, 0));
        return sizeToLevel;
    }

    /**
     * 将合并后的文本行转为 Markdown：标题用 #，段落用普通文本，换页用空行分隔
     */
    private static String toMarkdown(List<TextLine> lines, Map<Float, Integer> fontSizeToLevel) {
        StringBuilder md = new StringBuilder();
        for (TextLine line : lines) {
            String text = line.text().trim();
            if (text.isEmpty() || text.equals("\n")) {
                md.append("\n\n");
                continue;
            }
            int level = fontSizeToLevel.getOrDefault(line.fontSize(), 0);
            if (level > 0) {
                md.append("#".repeat(level)).append(" ").append(text).append("\n\n");
            } else {
                md.append(text).append("\n\n");
            }
        }
        return md.toString().trim();
    }

    /**
     * 合并连续具有相同字体和字号的文本行
     */
    private static List<TextLine> mergeConsecutiveLines(List<TextLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }
        List<TextLine> merged = new ArrayList<>();
        TextLine current = lines.getFirst();
        merged.add(current);
        for (int i = 1; i < lines.size(); i++) {
            TextLine next = lines.get(i);
            if (isSameStyle(current, next)) {
                String combinedText = current.text() + next.text();
                current = new TextLine(current.fontStyle(), combinedText, current.fontSize());
                merged.set(merged.size() - 1, current);
            } else {
                merged.add(next);
                current = next;
            }
        }
        return merged;
    }

    private static boolean isSameStyle(TextLine a, TextLine b) {
        if (a == null || b == null) return false;
        return a.fontSize() == b.fontSize() &&
                a.fontStyle().equals(b.fontStyle());
    }

    public record TextLine(String fontStyle, String text, float fontSize) {
        public TextLine(String fontStyle, String text, float fontSize) {
            this.fontStyle = fontStyle != null ? fontStyle : "unknown";
            this.text = text != null ? text : "";
            this.fontSize = fontSize;
        }
    }
}