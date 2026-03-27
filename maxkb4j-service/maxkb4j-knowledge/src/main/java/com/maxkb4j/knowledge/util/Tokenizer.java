package com.maxkb4j.knowledge.util;

import com.huaban.analysis.jieba.JiebaSegmenter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 内容分词工具类
 * 统一处理中文分词逻辑，避免代码重复
 * TextSegmented
 * @author tarzan
 * @date 2026-03-02
 */
public final class Tokenizer {

    private static final JiebaSegmenter Segment = new JiebaSegmenter();

    /**
     * 对文本进行分词处理
     *
     * @param text 待分词的文本
     * @return 分词后的文本，单词间用空格分隔
     */
    public static String segment(String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        List<String> tokens = Segment.sentenceProcess(text);
        return String.join(" ", tokens);
    }
}
