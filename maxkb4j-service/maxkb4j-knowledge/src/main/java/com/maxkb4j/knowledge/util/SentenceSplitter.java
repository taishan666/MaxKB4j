package com.maxkb4j.knowledge.util;

import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentenceSplitter {

    // 匹配 Markdown 图片: ![alt](url) 或 ![alt](url "title")
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\([^)]+\\)");

    /**
     * 使用 BreakIterator 按句子分割，并按 limit 合并为段落，同时忽略 Markdown 图片中的 !
     *
     * @param text   输入文本
     * @param limit  每段最大字符数（>0）
     * @param locale 语言区域（如 Locale.ENGLISH, Locale.CHINESE）
     * @return 分段后的列表
     */
    public static List<String> split(String text, int limit, Locale locale) {
        return split(text, limit, locale, 0);
    }

    /**
     * 使用 BreakIterator 按句子分割并按 limit 合并为段落，相邻段落回填 overlap 字符的句子上下文。
     *
     * @param text    输入文本
     * @param limit   每段最大字符数（>0）
     * @param overlap 相邻段落重叠的字符数上限（<=0 表示不重叠）
     * @return 分段后的列表
     */
    public static List<String> split(String text, int limit, int overlap) {
        return split(text, limit, Locale.getDefault(), overlap);
    }

    /**
     * @param locale 语言区域
     * @param overlap 相邻段落重叠的字符数上限（<=0 表示不重叠）
     */
    public static List<String> split(String text, int limit, Locale locale, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }

        // Step 1: 提取并替换所有 Markdown 图片为占位符
        Map<String, String> placeholderToImage = new LinkedHashMap<>();
        String processedText = replaceImagesWithPlaceholders(text, placeholderToImage);

        // Step 2: 使用 BreakIterator 分割句子
        BreakIterator sentenceIter = BreakIterator.getSentenceInstance(locale);
        sentenceIter.setText(processedText);
        List<String> sentences = new ArrayList<>();
        int start = sentenceIter.first();
        int end = sentenceIter.next();
        while (end != BreakIterator.DONE) {
            String sentence = processedText.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            start = end;
            end = sentenceIter.next();
        }
        // Step 3: 合并为段落（带 overlap 回填）
        List<String> paragraphs = TextSplitter.mergeChunksIntoParts(sentences, limit, "", overlap);
        // Step 4: 还原占位符为原始图片
        return restoreImagesFromPlaceholders(paragraphs, placeholderToImage);
    }

    /**
     * 将 Markdown 图片替换为唯一占位符，并记录映射
     */
    private static String replaceImagesWithPlaceholders(String text, Map<String, String> placeholderToImage) {
        Matcher matcher = MARKDOWN_IMAGE_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (matcher.find()) {
            String image = matcher.group();
            String placeholder = "{{IMG_" + (index++) + "}}";
            placeholderToImage.put(placeholder, image);
            matcher.appendReplacement(sb, placeholder);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将段落中的占位符还原为原始 Markdown 图片
     */
    private static List<String> restoreImagesFromPlaceholders(List<String> paragraphs, Map<String, String> placeholderToImage) {
        List<String> restored = new ArrayList<>();
        for (String para : paragraphs) {
            String restoredPara = para;
            for (Map.Entry<String, String> entry : placeholderToImage.entrySet()) {
                restoredPara = restoredPara.replace(entry.getKey(), entry.getValue());
            }
            restored.add(restoredPara);
        }
        return restored;
    }

}