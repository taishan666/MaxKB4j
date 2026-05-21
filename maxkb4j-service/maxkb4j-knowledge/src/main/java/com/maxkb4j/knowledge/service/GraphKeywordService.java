package com.maxkb4j.knowledge.service;

import com.maxkb4j.common.domain.dto.DualKeywords;
import com.maxkb4j.core.assistant.DualKeywordExtractionAssistant;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    public List<String>  extractKeywords(ChatModel chatModel, String query) {
        if (StringUtils.isBlank(query)) {
            return fallbackKeywordExtraction(query);
        }

        try {
            DualKeywordExtractionAssistant assistant = AssistantServices.builder(DualKeywordExtractionAssistant.class)
                    .chatModel(chatModel)
                    .build();
            DualKeywords dualKeywords= assistant.extractKeywords(query).content();
            List<String> keywords = new ArrayList<>();
            keywords.addAll(dualKeywords.getHighLevelKeywords());
            keywords.addAll(dualKeywords.getLowLevelKeywords());
            return keywords;
        } catch (Exception e) {
            log.warn("LLM keyword extraction failed, falling back to simple splitting: {}", e.getMessage());
            return fallbackKeywordExtraction(query);
        }
    }

    /**
     * Extract dual-level keywords using chatModelId
     */
    public List<String> extractKeywords(String chatModelId, String query) {
        if (StringUtils.isBlank(chatModelId)) {
            return fallbackKeywordExtraction(query);
        }
        try {
            ChatModel chatModel = modelProviderService.buildChatModel(chatModelId);
            return extractKeywords(chatModel, query);
        } catch (Exception e) {
            log.warn("Failed to build chat model for keyword extraction: {}", e.getMessage());
            return fallbackKeywordExtraction(query);
        }
    }

    /**
     * Fallback: simple word-based keyword extraction without LLM
     */
    public List<String> fallbackKeywordExtraction(String query) {
        if (StringUtils.isBlank(query)) {
            return List.of();
        }
        // Split query into individual words for low-level (entity) matching
        List<String> keywords = Arrays.stream(query.split("[\\s,，。.!？?；;：:]+"))
                .filter(StringUtils::isNotBlank)
                .filter(w -> w.length() > 1)
                .distinct()
                .collect(Collectors.toList());
        keywords.add(query.trim());
        return keywords;
    }

}