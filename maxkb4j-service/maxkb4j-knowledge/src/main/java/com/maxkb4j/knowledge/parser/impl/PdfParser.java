package com.maxkb4j.knowledge.parser.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.benjaminwan.ocrlibrary.OcrResult;
import com.maxkb4j.knowledge.parser.DocumentParser;
import com.maxkb4j.common.exception.FileLimitExceededException;
import com.maxkb4j.oss.service.IOssService;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/**
 * PDF 解析器。
 * <p>
 * PDFTextStripper 及其解析过程状态（页面行数据、待上传图片等）不是线程安全的，
 * 因此将所有可变状态封装到 {@link TextLineStripper} 中，每次解析新建实例；
 * 本组件自身保持无状态，可安全并发调用。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PdfParser implements DocumentParser {

    private final IOssService ossService;

    /** 单个 PDF 允许的最大尺寸（MB），超限拒绝解析，防止超大文件耗尽内存。 */
    @Value("${knowledge.pdf.max-size-mb:100}")
    private int maxPdfSizeMb;

    private static final String IMAGE_STYLE = "IMAGE";
    private static final InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
    private static final Pattern PAGE_NUM_DASH = Pattern.compile("^—\\d+—$");
    private static final Pattern PAGE_NUM_PURE = Pattern.compile("^\\d+$");
    private static final int MAX_TITLE_SIZE=100;

    @Override
    public List<String> getExtensions() {
        return List.of(".pdf");
    }

    @Override
    public String handle(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length > maxPdfSizeMb * 1024L * 1024L) {
                throw new FileLimitExceededException("common.file.size.exceeded", maxPdfSizeMb);
            }
            TextLineStripper stripper;
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (isScannedPDF(document)) {
                    return extractTextFromScannedPDF(document);
                }
                stripper = new TextLineStripper();
                stripper.setSortByPosition(true);
                stripper.setStartPage(1);
                stripper.setEndPage(document.getNumberOfPages());
                stripper.getText(document);
            }
            List<TextLine> lines = stripper.lines;
            uploadImagesInParallel(lines, stripper.pendingImages);
            HeadingContext ctx = buildFontSizeHeadingMap(lines);
            return toMarkdown(lines, ctx);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PDF from input stream", e);
        }
    }

    /** 图片上传最大并发数，避免对对象存储产生过大并行压力 */
    private static final int MAX_PARALLEL_UPLOADS = 8;

    /**
     * 并发上传 PDF 内图片。
     * <p>
     * 使用虚拟线程承载 IO 密集型的上传任务，并以信号量限制并发度；
     * 执行器通过 try-with-resources 关闭，解析失败或上传异常时也不会泄漏线程。
     * </p>
     */
    private void uploadImagesInParallel(List<TextLine> lines, List<ImageData> pendingImages) {
        if (pendingImages.isEmpty()) return;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore permits = new Semaphore(MAX_PARALLEL_UPLOADS);
            Map<String, String> urlMap = new ConcurrentHashMap<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (ImageData img : pendingImages) {
                futures.add(CompletableFuture.runAsync(() -> {
                    permits.acquireUninterruptibly();
                    try {
                        String imageUrl = ossService.uploadAndGetFileUrl(img.fileName(), img.bytes());
                        urlMap.put(img.fileName(), imageUrl);
                    } finally {
                        permits.release();
                    }
                }, executor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for (int i = 0; i < lines.size(); i++) {
                TextLine line = lines.get(i);
                if (IMAGE_STYLE.equals(line.fontStyle()) && urlMap.containsKey(line.text())) {
                    lines.set(i, new TextLine(IMAGE_STYLE, urlMap.get(line.text()), 0, 0, line.xPos(), line.yPos()));
                }
            }
        } finally {
            pendingImages.clear();
        }
    }

    private static boolean isScannedPDF(PDDocument document) {
        int checkPages = Math.min(3, document.getNumberOfPages());
        try {
            PDFTextStripper checkStripper = new PDFTextStripper();
            checkStripper.setStartPage(1);
            checkStripper.setEndPage(checkPages);
            String text = checkStripper.getText(document);
            String cleanText = text.replaceAll("\\s+", "");
            return cleanText.trim().length() < 10;
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
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

    private static byte[] bufferedImageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baoStream = new ByteArrayOutputStream();
        BufferedImage rgbImage = ensureRgbColorSpace(image);
        ImageIO.write(rgbImage, "png", baoStream);
        return baoStream.toByteArray();
    }

    private static BufferedImage ensureRgbColorSpace(BufferedImage image) {
        if (image.getColorModel().getNumColorComponents() <= 3
                && !image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgbImage;
    }

    /**
     * 单次解析的工作器：持有解析过程的全部可变状态。
     * 每次解析新建实例，避免单例共享状态导致的并发问题。
     */
    private static final class TextLineStripper extends PDFTextStripper {

        private final List<TextLine> lines = new ArrayList<>();
        private final List<ImageData> pendingImages = new ArrayList<>();
        private float currentPageHeight;
        private List<TextLine> currentPageLines = new ArrayList<>();
        private List<TextLine> currentPageImages = new ArrayList<>();

        private TextLineStripper() throws IOException {
            super();
        }

        @Override
        public void processPage(PDPage page) throws IOException {
            currentPageHeight = page.getCropBox().getHeight();
            currentPageImages = new ArrayList<>();
            currentPageLines = new ArrayList<>();
            super.processPage(page);
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            if ("Do".equals(operator.getName()) && !operands.isEmpty() && operands.getFirst() instanceof COSName cosName) {
                PDResources res = getResources();
                if (res != null && res.isImageXObject(cosName)) {
                    PDImageXObject image = (PDImageXObject) res.getXObject(cosName);
                    handleImageInStream(image);
                    return;
                }
            }
            super.processOperator(operator, operands);
        }

        @Override
        protected void endPage(PDPage page) throws IOException {
            List<TextLine> mergedLines = mergeConsecutiveLines(currentPageLines);
            if (CollectionUtils.isNotEmpty(mergedLines)) {
                clearPageNumber(mergedLines, PAGE_NUM_DASH);
                clearPageNumber(mergedLines, PAGE_NUM_PURE);
            }
            for (TextLine currentPageImage : currentPageImages) {
                float imgYPos = currentPageImage.yPos();
                for (int i = 0; i < mergedLines.size(); i++) {
                    TextLine textLine = mergedLines.get(i);
                    float yPos = textLine.yPos();
                    if (yPos > imgYPos) {
                        mergedLines.set(i, currentPageImage);
                        break;
                    }
                }
            }
            lines.addAll(mergedLines);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            if (text == null || text.isEmpty() || textPositions == null || textPositions.isEmpty()) {
                return;
            }
            TextPosition first = textPositions.getFirst();
            String fontName = getFontName(first.getFont());
            float fontSize = first.getFontSizeInPt();
            float xPos = first.getXDirAdj();
            float yPos = first.getYDirAdj();
            float maxHeight = first.getHeight();
            TextLine textLine = new TextLine(fontName, text, fontSize, maxHeight, xPos, yPos);
            currentPageLines.add(textLine);
        }

        private void handleImageInStream(PDImageXObject image) throws IOException {
            float translateX = getGraphicsState().getCurrentTransformationMatrix().getTranslateX();
            float translateY = getGraphicsState().getCurrentTransformationMatrix().getTranslateY();
            float yPos = currentPageHeight - translateY;
            BufferedImage bufferedImage = image.getImage();
            byte[] imageBytes = bufferedImageToBytes(bufferedImage);
            int pageNo = getCurrentPageNo();
            int imgIndex = currentPageImages.size();
            String fileName = "pdf_p" + pageNo + "_img" + imgIndex + ".png";
            currentPageImages.add(new TextLine(IMAGE_STYLE, fileName, 0, 0, translateX, yPos));
            pendingImages.add(new ImageData(fileName, imageBytes));
        }

        private static void clearPageNumber(List<TextLine> lines, Pattern pattern) {
            if (CollectionUtils.isNotEmpty(lines)) {
                TextLine first = lines.getFirst();
                TextLine last = lines.getLast();
                if (pattern.matcher(first.text()).matches()) {
                    lines.removeFirst();
                }
                if (lines.isEmpty()) return;
                if (pattern.matcher(last.text()).matches()) {
                    lines.removeLast();
                }
            }
        }

        private static String getFontName(PDFont font) {
            if (font == null) {
                return "unknown";
            }
            try {
                return font.getName();
            } catch (Exception e) {
                return "unknown";
            }
        }
    }

    /**
     * 统计字号频率确定正文基准，结合字体粗体特征映射标题层级
     */
    private static HeadingContext buildFontSizeHeadingMap(List<TextLine> lines) {
        // 统计非换行、非零字号的频率
        Map<Float, Integer> freq = new LinkedHashMap<>();
        for (TextLine line : lines) {
            if (line.fontSize() < MAX_TITLE_SIZE) {
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
            if (line.fontSize() == bodyFontSize) {
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
     * 将合并后的文本行转为 Markdown：标题用 #，段落用普通文本，图片用 ![](url)
     */
    private static String toMarkdown(List<TextLine> lines, HeadingContext ctx) {
        StringBuilder md = new StringBuilder();
        for (TextLine line : lines) {
            if (IMAGE_STYLE.equals(line.fontStyle())) {
                md.append("![](").append(line.text()).append(")").append("\n");
                continue;
            }
            String text = line.text().trim();
            if (text.isEmpty() || text.equals("\n")) {
                md.append("\n");
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
            if (level > 0 && line.xPos() < 150) {
                md.append("#".repeat(level)).append(" ").append(text).append("\n\n");
            } else {
                md.append(text).append("\n");
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
            if (isSameRow(current, next)) {
                String combinedText = current.text() + next.text();
                current = new TextLine(next.fontStyle(), combinedText, next.fontSize(), next.maxHeight(), current.xPos(), next.yPos());
                merged.set(merged.size() - 1, current);
            } else {
                merged.add(next);
                current = next;
            }
        }
        return merged;
    }

    private static boolean isSameRow(TextLine a, TextLine b) {
        if (a == null || b == null) return false;
        float diff = Math.abs(a.yPos - b.yPos);
        return diff <= 2.2;
    }

    private static boolean isBoldFont(String fontName) {
        if (fontName == null || fontName.isEmpty()) return false;
        String lower = fontName.toLowerCase();
        return lower.contains("simhei") || lower.contains("heiti") || lower.contains("bold") || fontName.contains("黑体");
    }

    private record HeadingContext(
            Map<Float, Integer> fontSizeToLevel,
            float bodyFontSize,
            boolean bodyIsBold,
            int boldAtBaselineLevel
    ) {
    }

    public record TextLine(String fontStyle, String text, float fontSize, float maxHeight, float xPos, float yPos) {
        public TextLine(String fontStyle, String text, float fontSize, float maxHeight, float xPos, float yPos) {
            this.fontStyle = fontStyle != null ? fontStyle : "unknown";
            this.text = text != null ? text : "";
            this.fontSize = fontSize;
            this.maxHeight = maxHeight;
            this.xPos = xPos;
            this.yPos = yPos;
        }
    }

    private record ImageData(String fileName, byte[] bytes) {}
}
