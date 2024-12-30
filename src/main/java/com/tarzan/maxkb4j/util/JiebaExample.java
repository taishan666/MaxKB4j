package com.tarzan.maxkb4j.util;

import com.huaban.analysis.jieba.JiebaSegmenter;

public class JiebaExample {
    public static void main(String[] args) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        String text = "我爱自然语言处理";

        // 精确模式
        System.out.println("精确模式: " + segmenter.sentenceProcess(text));

        // 全模式
        System.out.println("全模式: " + segmenter.process(text, JiebaSegmenter.SegMode.INDEX));
    }
}