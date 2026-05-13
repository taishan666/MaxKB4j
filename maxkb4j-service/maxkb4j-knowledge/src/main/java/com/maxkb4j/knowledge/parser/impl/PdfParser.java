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
                HeadingContext ctx = buildFontSizeHeadingMap(mergedLines);
                return toMarkdown(mergedLines, ctx);
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
     * 统计字号频率确定正文基准，结合字体粗体特征映射标题层级
     */
    private static HeadingContext buildFontSizeHeadingMap(List<TextLine> lines) {
        // 统计非换行、非零字号的频率
        Map<Float, Integer> freq = new LinkedHashMap<>();
        for (TextLine line : lines) {
            if (line.fontSize() > 0 && !line.text().equals("\n")) {
                freq.merge(line.fontSize(), 1, Integer::sum);
            }
        }
        if (freq.isEmpty()) {
            return new HeadingContext(Map.of(), 12f, false, 1);
        }
        // 找到出现次数最多的字号作为正文基准
        float bodyFontSize = freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(12f);

        // 统计正文基准字号下各字体名频率，判断正文主流字体是否粗体
        Map<String, Integer> fontFreqAtBodySize = new LinkedHashMap<>();
        for (TextLine line : lines) {
            if (line.fontSize() == bodyFontSize && !line.text().equals("\n")) {
                fontFreqAtBodySize.merge(line.fontStyle(), 1, Integer::sum);
            }
        }
        String bodyFontName = fontFreqAtBodySize.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
        boolean bodyIsBold = isBoldFont(bodyFontName);

        // 比基准字号大的按从大到小排序，映射为 h1 ~ h6
        List<Float> headingSizes = freq.keySet().stream()
                .filter(s -> s > bodyFontSize)
                .sorted(Comparator.reverseOrder())
                .toList();

        Map<Float, Integer> sizeToLevel = new LinkedHashMap<>();
        for (int i = 0; i < headingSizes.size() && i < 6; i++) {
            sizeToLevel.put(headingSizes.get(i), i + 1);
        }
        // 基准字号及更小的字号 → 段落（level=0）
        sizeToLevel.put(bodyFontSize, 0);
        freq.keySet().stream()
                .filter(s -> s < bodyFontSize && s > 0)
                .forEach(s -> sizeToLevel.put(s, 0));

        // 粗体正文基准字号标题层级 = 字号标题最大层级 + 1
        int maxHeadingLevel = sizeToLevel.values().stream()
                .filter(l -> l > 0)
                .max(Integer::compare)
                .orElse(0);
        int boldAtBaselineLevel = Math.min(maxHeadingLevel + 1, 6);

        return new HeadingContext(sizeToLevel, bodyFontSize, bodyIsBold, boldAtBaselineLevel);
    }

    /**
     * 将合并后的文本行转为 Markdown：标题用 #，段落用普通文本，换页用空行分隔
     */
    private static String toMarkdown(List<TextLine> lines, HeadingContext ctx) {
        StringBuilder md = new StringBuilder();
        for (TextLine line : lines) {
            String text = line.text().trim();
            if (text.isEmpty() || text.equals("\n")) {
                md.append("\n\n");
                continue;
            }
            int level = ctx.fontSizeToLevel().getOrDefault(line.fontSize(), 0);
            // 粗体正文基准字号 → 子标题
            if (level == 0
                    && line.fontSize() == ctx.bodyFontSize()
                    && isBoldFont(line.fontStyle())
                    && !ctx.bodyIsBold()) {
                level = ctx.boldAtBaselineLevel();
            }
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

    private static boolean isBoldFont(String fontName) {
        if (fontName == null || fontName.isEmpty()) return false;
        String lower = fontName.toLowerCase();
        if (lower.contains("bold")) return true;
        if (lower.contains("simhei") || lower.contains("heiti")) return true;
        if (fontName.contains("黑体")) return true;
        return false;
    }

    private record HeadingContext(
            Map<Float, Integer> fontSizeToLevel,
            float bodyFontSize,
            boolean bodyIsBold,
            int boldAtBaselineLevel
    ) {}

    public record TextLine(String fontStyle, String text, float fontSize) {
        public TextLine(String fontStyle, String text, float fontSize) {
            this.fontStyle = fontStyle != null ? fontStyle : "unknown";
            this.text = text != null ? text : "";
            this.fontSize = fontSize;
        }
    }
}