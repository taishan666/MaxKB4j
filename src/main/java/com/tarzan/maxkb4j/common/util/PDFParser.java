package com.tarzan.maxkb4j.common.util;

import lombok.Getter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 PDF 文档解析为带有字体和字号信息的文本行，并最终合并为纯文本字符串。
 */
@Getter
public class PDFParser extends PDFTextStripper {

    private final List<TextLine> lines;

    public PDFParser() {
        super();
        this.lines = new ArrayList<>();
        setSortByPosition(true); // 确保按视觉顺序提取文本
    }

    @Override
    protected void writePage() throws IOException {
        super.writePage();
        // 添加换页标记（可选，用于调试或保留段落结构）
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

    /**
     * 安全获取字体名称
     */
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

    /**
     * 从 InputStream 解析 PDF 并返回合并后的纯文本（保留同格式连续文本）
     */
    public static String parse(InputStream inputStream) {
        try {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFParser converter = new PDFParser();
            converter.setStartPage(1);
            converter.setEndPage(document.getNumberOfPages());
            converter.getText(document); // 触发 writeString 调用

            List<TextLine> mergedLines = mergeConsecutiveLines(converter.getLines());
            return mergedLines.stream()
                    .map(TextLine::text)
                    .reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append)
                    .toString();
        }
        }catch (IOException e){
            throw new RuntimeException("Failed to parse PDF from input stream", e);
        }
    }

    /**
     * 合并连续具有相同字体和字号的文本行
     */
    private static List<TextLine> mergeConsecutiveLines(List<TextLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }

        List<TextLine> merged = new ArrayList<>();
        TextLine current = lines.get(0);
        merged.add(current);

        for (int i = 1; i < lines.size(); i++) {
            TextLine next = lines.get(i);
            if (isSameStyle(current, next)) {
                // 合并文本（保留原始内容，不强制加空格）
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

    // ---------------- 内部数据类 ----------------

    public record TextLine(String fontStyle, String text, float fontSize) {
            public TextLine(String fontStyle, String text, float fontSize) {
                this.fontStyle = fontStyle != null ? fontStyle : "unknown";
                this.text = text != null ? text : "";
                this.fontSize = fontSize;
            }
        }
}