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
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
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
 * 解析过程状态（页面行数据、待上传图片等）不是线程安全的，
 * 因此将所有可变状态封装到每次解析新建的实例中；
 * 本组件自身保持无状态，可安全并发调用。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PdfParser implements DocumentParser {

    private static final String IMAGE_STYLE = "IMAGE";
    private static final InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);
    private static final Pattern PAGE_NUM_DASH = Pattern.compile("^—\\d+—$");
    private static final Pattern PAGE_NUM_PURE = Pattern.compile("^\\d+$");
    private static final int MAX_TITLE_SIZE = 100;
    /**
     * 图片上传最大并发数，避免对对象存储产生过大并行压力
     */
    private static final int MAX_PARALLEL_UPLOADS = 8;
    private final IOssService ossService;
    /**
     * 单个 PDF 允许的最大尺寸（MB），超限拒绝解析，防止超大文件耗尽内存。
     */
    @Value("${knowledge.pdf.max-size-mb:100}")
    private int maxPdfSizeMb;

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

            // 第一步：提取所有图片（XObject 图像与内联图像统一由 drawImage 回调捕获）
            List<ImageData> pendingImages = new ArrayList<>();
            Map<Integer, List<TextLine>> pageImages = new LinkedHashMap<>();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (isScannedPDF(document)) {
                    return extractTextFromScannedPDF(document);
                }
                int pageNo = 0;
                for (PDPage page : document.getPages()) {
                    pageNo++;
                    ImageExtractor imageExtractor = new ImageExtractor(page, pageNo, pendingImages, pageImages);
                    imageExtractor.processPage(page);
                }
            }

            // 第二步：提取文本
            TextLineStripper stripper;
            try (PDDocument document = Loader.loadPDF(bytes)) {
                stripper = new TextLineStripper();
                stripper.setSortByPosition(true);
                stripper.setStartPage(1);
                stripper.setEndPage(document.getNumberOfPages());
                stripper.getText(document);
            }
            List<TextLine> lines = stripper.lines;

            // 第三步：按 Y 坐标将图片插入到对应页面的文本行中
            insertImagesIntoLines(lines, stripper.pageStartIndices, stripper.pageEndIndices, pageImages);

            // 第四步：上传图片并生成 Markdown
            uploadImagesInParallel(lines, pendingImages);
            HeadingContext ctx = buildFontSizeHeadingMap(lines);
            return toMarkdown(lines, ctx);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PDF from input stream", e);
        }
    }

    /**
     * 将图片行按 Y 坐标插入到对应页面的文本行中，
     * 图片插入到首个 Y 坐标大于图片的文本行之前，保持阅读顺序。
     * <p>
     * 注意：向 lines 插入元素会使后续页面的边界索引整体后移，
     * 因此按页号升序处理并累计已插入数量对边界索引做补偿。
     * </p>
     */
    private static void insertImagesIntoLines(List<TextLine> lines,
                                              Map<Integer, Integer> pageStartIndices,
                                              Map<Integer, Integer> pageEndIndices,
                                              Map<Integer, List<TextLine>> pageImages) {
        int offset = 0;
        for (Map.Entry<Integer, List<TextLine>> entry : pageImages.entrySet()) {
            int pageNum = entry.getKey();
            int rawStart = pageStartIndices.getOrDefault(pageNum, -1);
            if (rawStart < 0) {
                continue;
            }
            int pageStartIdx = rawStart + offset;
            int pageEndIdx = Math.min(pageEndIndices.getOrDefault(pageNum, lines.size()) + offset, lines.size());
            for (TextLine imgLine : entry.getValue()) {
                float imgYPos = imgLine.yPos();
                int insertIdx = pageEndIdx;
                for (int i = pageStartIdx; i < pageEndIdx; i++) {
                    if (lines.get(i).yPos() > imgYPos) {
                        insertIdx = i;
                        break;
                    }
                }
                lines.add(insertIdx, imgLine);
                pageEndIdx++;
                offset++;
            }
        }
    }

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

    /**
     * 图片提取器：基于 {@link PDFGraphicsStreamEngine}，
     * PDFBox 会在解析内容流时对 XObject 图像（Do 操作符）和内联图像（BI/ID/EI 操作符）
     * 统一回调 {@link #drawImage(PDImage)}，因此两种嵌入方式均可捕获。
     * 每页创建一个实例，通过构造器共享结果集合。
     */
    private static class ImageExtractor extends PDFGraphicsStreamEngine {
        private final List<ImageData> pendingImages;
        private final Map<Integer, List<TextLine>> pageImages;
        private final int pageNo;
        private final PDRectangle cropBox;

        ImageExtractor(PDPage page, int pageNo,
                       List<ImageData> pendingImages,
                       Map<Integer, List<TextLine>> pageImages) {
            super(page);
            this.pageNo = pageNo;
            this.cropBox = page.getCropBox();
            this.pendingImages = pendingImages;
            this.pageImages = pageImages;
        }

        @Override
        public void drawImage(PDImage pdImage) throws IOException {
            BufferedImage image = pdImage.getImage();
            if (image == null) {
                log.debug("图像无法解码，跳过：宽度={}, 高度={}", pdImage.getWidth(), pdImage.getHeight());
                return;
            }
            /*
             * 坐标对齐说明（与 PDFTextStripper 的 TextPosition 保持同一坐标系）：
             * 文本提取时坐标基于 CropBox 并规范化到其原点（减 lowerLeftX/lowerLeftY），
             * Y 为"页面顶部到基线"的 top-down 距离（cropBox.height - 用户空间Y）。
             * 图像同理：将图像单位正方形的四个角点经 CTM 变换取包围盒，
             * X 取左边缘相对 CropBox 左侧的距离，Y 取"顶部边缘"距页面上边缘的距离，
             * 这样图片与文本的 Y 可直接比较，插入位置与视觉阅读顺序一致。
             */
            Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            for (float[] corner : new float[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}}) {
                Point2D.Float p = ctm.transformPoint(corner[0], corner[1]);
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
            float xPos = minX - cropBox.getLowerLeftX();
            float yPos = cropBox.getHeight() - (maxY - cropBox.getLowerLeftY());
            byte[] imageBytes = bufferedImageToBytes(image);
            String fileName = "pdf_p" + pageNo + "_img" + pendingImages.size() + ".png";
            pageImages.computeIfAbsent(pageNo, k -> new ArrayList<>())
                    .add(new TextLine(IMAGE_STYLE, fileName, 0, 0, xPos, yPos));
            pendingImages.add(new ImageData(fileName, imageBytes));
        }

        // 以下为 PDFGraphicsStreamEngine 的抽象方法，图片提取场景无需处理图形路径，给空实现
        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        }

        @Override
        public void clip(int windingRule) {
        }

        @Override
        public void moveTo(float x, float y) {
        }

        @Override
        public void lineTo(float x, float y) {
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float(0, 0);
        }

        @Override
        public void closePath() {
        }

        @Override
        public void endPath() {
        }

        @Override
        public void strokePath() {
        }

        @Override
        public void fillPath(int windingRule) {
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
        }

        @Override
        public void shadingFill(COSName shadingName) {
        }
    }

    /**
     * 单次文本解析的工作器：持有解析过程的全部可变状态。
     * 每次解析新建实例，避免单例共享状态导致的并发问题。
     */
    private static final class TextLineStripper extends PDFTextStripper {

        private final List<TextLine> lines = new ArrayList<>();
        private final Map<Integer, Integer> pageStartIndices = new HashMap<>();
        private final Map<Integer, Integer> pageEndIndices = new HashMap<>();
        private final List<TextLine> currentPageLines = new ArrayList<>();
        /**
         * 页码计数器，从 1 开始，与 {@link ImageExtractor} 的页编号保持一致
         */
        private int pageCounter = 0;

        private TextLineStripper() throws IOException {
            super();
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

        @Override
        public void processPage(PDPage page) throws IOException {
            pageCounter++;
            pageStartIndices.put(pageCounter, lines.size());
            currentPageLines.clear();
            super.processPage(page);
        }

        @Override
        protected void endPage(PDPage page) throws IOException {
            List<TextLine> mergedLines = mergeConsecutiveLines(currentPageLines);
            if (CollectionUtils.isNotEmpty(mergedLines)) {
                clearPageNumber(mergedLines, PAGE_NUM_DASH);
                clearPageNumber(mergedLines, PAGE_NUM_PURE);
            }
            lines.addAll(mergedLines);
            pageEndIndices.put(pageCounter, lines.size());
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

    private record ImageData(String fileName, byte[] bytes) {
    }
}
