package com.maxkb4j.knowledge.service;

import com.maxkb4j.common.domain.dto.DualKeywords;
import com.maxkb4j.core.assistant.DualKeywordExtractionAssistant;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ParagraphKeywords;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordsExtractionService {

    private final MongoTemplate mongoTemplate;


    public void extractFromDocument(ChatModel chatModel, String knowledgeId, String documentId, List<ParagraphEntity> paragraphs) {
        if (CollectionUtils.isEmpty(paragraphs)) return;
        List<ParagraphKeywords> paragraphKeywordsList = new ArrayList<>();
        for (ParagraphEntity paragraph : paragraphs) {
            if (paragraph.getContent() == null || paragraph.getContent().isBlank()) {
                log.warn("Paragraph content is empty, skipping extraction. Paragraph ID: {}", paragraph.getId());
                return;
            }
            log.info("Extracting entities/relationships from paragraph [{}]", paragraph.getId());
            DualKeywordExtractionAssistant assistant = AssistantServices.builder(DualKeywordExtractionAssistant.class)
                    .chatModel(chatModel)
                    .build();
            DualKeywords  dualKeywords= assistant.extractKeywords(paragraph.getTitle()+":"+paragraph.getContent()).content();
            List<String> keywords = new ArrayList<>();
            keywords.addAll(dualKeywords.getHighLevelKeywords());
            keywords.addAll(dualKeywords.getLowLevelKeywords());
            ParagraphKeywords paragraphKeywords = ParagraphKeywords.builder()
                    .keywords(String.join(",", keywords))
                    .knowledgeId(knowledgeId)
                    .documentId(documentId)
                    .isActive(true)
                    .paragraphId(paragraph.getId())
                    .build();
            paragraphKeywordsList.add(paragraphKeywords);
        }
        mongoTemplate.insertAll(paragraphKeywordsList);
    }

}