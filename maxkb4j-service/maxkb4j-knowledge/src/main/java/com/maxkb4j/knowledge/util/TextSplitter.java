package com.maxkb4j.knowledge.util;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {

    /**
     * 将分块列表合并为不超过 limit 的段落（无重叠）。
     */
    public static List<String> mergeChunksIntoParts(List<String> chunks, int limit, String join) {
        return mergeChunksIntoParts(chunks, limit, join, 0);
    }

    /**
     * 将分块列表合并为不超过 limit 的段落，相邻段落间按句子级回填 overlap 字符的上下文。
     *
     * <p>重叠缓解"跨块边界信息丢失"：位于块边界处的句子在前后两个块中都出现，
     * 检索时以任一半提问均可命中完整上下文。overlap 按 chunk（句子/行）整块累计，
     * 只回填能完整放入预算的 chunk，不做字符级截断。</p>
     *
     * @param chunks   待合并的块（句子或行）
     * @param limit    每段最大字符数
     * @param join     块之间的连接符
     * @param overlap  相邻段落重叠的字符数上限（<=0 表示不重叠）
     */
    public static List<String> mergeChunksIntoParts(List<String> chunks, int limit, String join, int overlap) {
        List<String> paragraphs = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int currentLen = 0;
        for (String chunk : chunks) {
            int chunkLen = chunk.length() + join.length();
            if (!current.isEmpty() && currentLen + chunkLen > limit) {
                paragraphs.add(joinChunks(current, join));
                current = tailChunks(current, overlap);
                currentLen = totalLength(current, join);
            }
            current.add(chunk);
            currentLen += chunkLen;
        }
        if (!current.isEmpty()) {
            paragraphs.add(joinChunks(current, join));
        }
        return paragraphs;
    }

    /**
     * 从已合并的块末尾向前收集累计长度不超过 overlap 的块，作为下一段的开头上下文。
     */
    private static List<String> tailChunks(List<String> chunks, int overlap) {
        List<String> tail = new ArrayList<>();
        int tailLen = 0;
        for (int i = chunks.size() - 1; i >= 0; i--) {
            int len = chunks.get(i).length();
            if (tailLen + len > overlap) {
                break;
            }
            tail.add(0, chunks.get(i));
            tailLen += len;
        }
        return tail;
    }

    private static String joinChunks(List<String> chunks, String join) {
        StringBuilder sb = new StringBuilder();
        for (String chunk : chunks) {
            sb.append(chunk).append(join);
        }
        return sb.toString();
    }

    private static int totalLength(List<String> chunks, String join) {
        int len = 0;
        for (String chunk : chunks) {
            len += chunk.length() + join.length();
        }
        return len;
    }
}
