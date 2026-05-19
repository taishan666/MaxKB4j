package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.core.assistant.DualKeywordExtractionAssistant;
import com.maxkb4j.core.dto.DualKeywordResult;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphKeywordService {

    private final IModelProviderService modelProviderService;

    /**
     * Extract dual-level keywords using LLM
     */
    public DualKeywordResult extractDualKeywords(ChatModel chatModel, String query) {
        if (StringUtils.isBlank(query)) {
            return fallbackKeywordExtraction(query);
        }

        try {
            DualKeywordExtractionAssistant assistant = AssistantServices.builder(DualKeywordExtractionAssistant.class)
                    .chatModel(chatModel)
                    .build();

            String rawResult = assistant.extractKeywords(query).content();
            return parseDualKeywordResult(rawResult);
        } catch (Exception e) {
            log.warn("LLM keyword extraction failed, falling back to simple splitting: {}", e.getMessage());
            return fallbackKeywordExtraction(query);
        }
    }

    /**
     * Extract dual-level keywords using chatModelId
     */
    public DualKeywordResult extractDualKeywords(String chatModelId, String query) {
        if (StringUtils.isBlank(chatModelId)) {
            return fallbackKeywordExtraction(query);
        }
        try {
            ChatModel chatModel = modelProviderService.buildChatModel(chatModelId);
            return extractDualKeywords(chatModel, query);
        } catch (Exception e) {
            log.warn("Failed to build chat model for keyword extraction: {}", e.getMessage());
            return fallbackKeywordExtraction(query);
        }
    }

    /**
     * Fallback: simple word-based keyword extraction without LLM
     */
    public DualKeywordResult fallbackKeywordExtraction(String query) {
        if (StringUtils.isBlank(query)) {
            return new DualKeywordResult(List.of(), List.of());
        }
        // Split query into individual words for low-level (entity) matching
        List<String> lowLevelKeywords = Arrays.stream(query.split("[\\s,，。.!？?；;：:]+"))
                .filter(StringUtils::isNotBlank)
                .filter(w -> w.length() > 1)
                .distinct()
                .collect(Collectors.toList());
        // Use full query as single high-level keyword for topic matching
        List<String> highLevelKeywords = List.of(query.trim());

        return new DualKeywordResult(highLevelKeywords, lowLevelKeywords);
    }

    private DualKeywordResult parseDualKeywordResult(String rawResult) {
        if (StringUtils.isBlank(rawResult)) {
            return new DualKeywordResult(List.of(), List.of());
        }
        try {
            String jsonStr = extractJson(rawResult);
            return JSON.parseObject(jsonStr, DualKeywordResult.class);
        } catch (Exception e) {
            log.warn("Failed to parse dual keyword result: {}", e.getMessage());
            return new DualKeywordResult(List.of(), List.of());
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}