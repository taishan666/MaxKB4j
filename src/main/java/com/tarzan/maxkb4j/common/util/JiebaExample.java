package com.tarzan.maxkb4j.common.util;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.common.base.dto.WordIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JiebaExample {
    public static void main(String[] args) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        String text = "MaxKB 是一款基于 LLM 大语言模型的知识库问答系统。MaxKB = Max Knowledge Base，旨在成为企业的最强大脑。\n" +
                "开箱即用：支持直接上传文档、自动爬取在线文档，支持文本自动拆分、向量化，智能问答交互体验好；\n" +
                "无缝嵌入：支持零编码快速嵌入到第三方业务系统；\n" +
                "多模型支持：支持对接主流的大模型，包括 Ollama 本地私有大模型（如 Llama 2、Llama 3、qwen）、通义千问、OpenAI、Azure OpenAI、Kimi、智谱 AI、讯飞星火和百度千帆大模型等。";

        // 精确模式
        List<String> segmentations = filterPunctuation(segmenter.sentenceProcess(text));
        System.out.println("精确模式: " + segmentations);
        System.out.println("-----------------------------------------------------");
        List<WordIndex> wordIndices = new ArrayList<>();
        for (int i = 0; i < segmentations.size(); i++) {
            WordIndex wordIndex = new WordIndex();
            wordIndex.setWord(segmentations.get(i));
            wordIndex.setIndex(i);
            wordIndices.add(wordIndex);
        }
        Map<String, List<WordIndex>> map = wordIndices.stream().collect(Collectors.groupingBy(WordIndex::getWord));
        StringBuilder result = new StringBuilder();
        map.forEach((k,v)->{
            result.append(k).append(":");
            StringBuilder sb = new StringBuilder();
            for (WordIndex w : v) {
                sb.append(w.getIndex()).append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            result.append(sb).append(" ");
        });
        //System.out.println(result);
    }

    private static List<String> filterPunctuation(List<String> words) {
        String[] filteredWords = {"", ",", ".", "。", "=", "，", "、", "：", "；", "（", "）"};
        List<String> result = new ArrayList<>();
        for (String word : words) {
            word = word.trim();
            for (String filteredWord : filteredWords) {
                if (word.contains(filteredWord)) {
                    word = word.replaceAll(filteredWord, "");
                }
            }
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        return result;
    }

}