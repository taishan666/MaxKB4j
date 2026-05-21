package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.core.assistant.EntityExtractionAssistant;
import com.maxkb4j.core.dto.ExtractionResult;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.knowledge.entity.GraphEntityEntity;
import com.maxkb4j.knowledge.entity.GraphRelationshipEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.mapper.GraphEntityMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphExtractionService {

    private final GraphStoreService graphStoreService;

    private final GraphEntityMapper graphEntityMapper;

    @Transactional(rollbackFor = Exception.class)
    public void extractFromParagraph(ChatModel chatModel, String knowledgeId, String documentId, ParagraphEntity paragraph) {
        if (paragraph.getContent() == null || paragraph.getContent().isBlank()) {
            log.warn("Paragraph content is empty, skipping extraction. Paragraph ID: {}", paragraph.getId());
            return;
        }

        log.info("Extracting entities/relationships from paragraph [{}]", paragraph.getId());

        // Build LLM assistant
        EntityExtractionAssistant assistant = AssistantServices.builder(EntityExtractionAssistant.class)
                .chatModel(chatModel)
                .build();

        // Call LLM for extraction
        String rawResult = assistant.extract(paragraph.getContent()).content();
        List<ExtractionResult> extractionResults = parseExtractionResult(rawResult);

        if (CollectionUtils.isEmpty(extractionResults)) {
            log.info("No entities/relationships extracted from paragraph [{}]", paragraph.getId());
            return;
        }

        // Separate entities and relationships
        List<ExtractionResult> entityResults = extractionResults.stream()
                .filter(ExtractionResult::isEntity).toList();
        List<ExtractionResult> relationshipResults = extractionResults.stream()
                .filter(ExtractionResult::isRelationship).toList();

        // Process entities: deduplicate and merge
        Map<String, GraphEntityEntity> processedEntities = processEntities(knowledgeId, documentId, paragraph.getId(), entityResults);

        // Process relationships
        List<GraphRelationshipEntity> processedRelationships = processRelationships(knowledgeId, documentId, paragraph.getId(), relationshipResults, processedEntities);

        log.info("Extracted {} entities and {} relationships from paragraph [{}]",
                processedEntities.size(), processedRelationships.size(), paragraph.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void extractFromDocument(ChatModel chatModel, String knowledgeId, String documentId, List<ParagraphEntity> paragraphs) {
        if (CollectionUtils.isEmpty(paragraphs)) return;
        for (ParagraphEntity paragraph : paragraphs) {
            try {
                extractFromParagraph(chatModel, knowledgeId, documentId, paragraph);
            } catch (Exception e) {
                log.error("Failed to extract from paragraph [{}]: {}", paragraph.getId(), e.getMessage(), e);
            }
        }
    }

    private List<ExtractionResult> parseExtractionResult(String rawResult) {
        if (StringUtils.isBlank(rawResult)) return Collections.emptyList();
        try {
            // Try to extract JSON array from the response (LLM may include extra text)
            String jsonStr = extractJsonArray(rawResult);
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            return jsonArray.stream()
                    .map(obj -> JSON.parseObject(obj.toString(), ExtractionResult.class))
                    .filter(r -> StringUtils.isNotBlank(r.getType()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse extraction result: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String extractJsonArray(String text) {
        // Find the JSON array in the response
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private Map<String, GraphEntityEntity> processEntities(String knowledgeId, String documentId,
                                                            String paragraphId,
                                                            List<ExtractionResult> entityResults) {
        Map<String, GraphEntityEntity> result = new LinkedHashMap<>();

        for (ExtractionResult er : entityResults) {
            if (StringUtils.isBlank(er.getName())) continue;

            String entityName = er.getName().trim();
            String entityType = StringUtils.isNotBlank(er.getEntityType()) ? er.getEntityType().trim() : "concept";
            String description = StringUtils.isNotBlank(er.getDescription()) ? er.getDescription().trim() : entityName;

            // Check for existing entity with same name in this knowledge base
            GraphEntityEntity existingEntity = graphStoreService.findEntityByName(knowledgeId, entityName);

            if (existingEntity != null) {
                // Entity exists: update description if new info adds value, and add paragraph mapping
                String existingDesc = existingEntity.getDescription();
                if (StringUtils.isNotBlank(description) && !description.equals(existingDesc)) {
                    existingEntity.setDescription(existingDesc + "; " + description);
                    updateEntityDescription(existingEntity);
                }
                // Ensure paragraph mapping exists
                graphStoreService.saveEntityParagraphMapping(existingEntity.getId(), paragraphId, knowledgeId, documentId);
                result.put(entityName, existingEntity);
            } else {
                // Create new entity with embedding
                String entityId = IdWorker.get32UUID();
                try {
                    GraphEntityEntity newEntity = new GraphEntityEntity();
                    newEntity.setId(entityId);
                    newEntity.setName(entityName);
                    newEntity.setEntityType(entityType);
                    newEntity.setDescription(description);
                    newEntity.setKnowledgeId(knowledgeId);
                    newEntity.setDocumentId(documentId);
                    newEntity.setIsActive(true);
                    graphStoreService.saveEntity(newEntity);
                    graphStoreService.saveEntityParagraphMapping(entityId, paragraphId, knowledgeId, documentId);
                    result.put(entityName, newEntity);
                } catch (Exception e) {
                    log.warn("Failed to create embedding for entity [{}]: {}", entityName, e.getMessage());
                    // Create entity without embedding as fallback
                    GraphEntityEntity newEntity = new GraphEntityEntity();
                    newEntity.setId(entityId);
                    newEntity.setName(entityName);
                    newEntity.setEntityType(entityType);
                    newEntity.setDescription(description);
                    newEntity.setKnowledgeId(knowledgeId);
                    newEntity.setDocumentId(documentId);
                    newEntity.setIsActive(true);
                    graphStoreService.saveEntity(newEntity);
                    graphStoreService.saveEntityParagraphMapping(entityId, paragraphId, knowledgeId, documentId);
                    result.put(entityName, newEntity);
                }
            }
        }

        return result;
    }

    private List<GraphRelationshipEntity> processRelationships(String knowledgeId, String documentId,
                                                                String paragraphId,
                                                                List<ExtractionResult> relationshipResults,
                                                                Map<String, GraphEntityEntity> processedEntities) {
        List<GraphRelationshipEntity> result = new ArrayList<>();

        for (ExtractionResult rr : relationshipResults) {
            if (StringUtils.isBlank(rr.getSourceEntity()) || StringUtils.isBlank(rr.getTargetEntity())) continue;

            String sourceName = rr.getSourceEntity().trim();
            String targetName = rr.getTargetEntity().trim();
            String description = StringUtils.isNotBlank(rr.getDescription()) ? rr.getDescription().trim() : "";
            String keywords = StringUtils.isNotBlank(rr.getKeywords()) ? rr.getKeywords().trim() : "";

            // Get source and target entity IDs
            GraphEntityEntity sourceEntity = processedEntities.get(sourceName);
            GraphEntityEntity targetEntity = processedEntities.get(targetName);

            // If entities weren't in this extraction, try to find existing ones
            if (sourceEntity == null) {
                sourceEntity = graphStoreService.findEntityByName(knowledgeId, sourceName);
            }
            if (targetEntity == null) {
                targetEntity = graphStoreService.findEntityByName(knowledgeId, targetName);
            }

            if (sourceEntity == null || targetEntity == null) {
                log.debug("Skipping relationship [{} -> {}]: entity not found", sourceName, targetName);
                continue;
            }

            String relationshipId = IdWorker.get32UUID();
            String contentForEmbedding = description + " " + keywords;

            try {

                GraphRelationshipEntity relationship = new GraphRelationshipEntity();
                relationship.setId(relationshipId);
                relationship.setSourceEntityId(sourceEntity.getId());
                relationship.setTargetEntityId(targetEntity.getId());
                relationship.setDescription(description);
                relationship.setKeywords(keywords);
                relationship.setWeight(1.0);
                relationship.setKnowledgeId(knowledgeId);
                relationship.setDocumentId(documentId);
                relationship.setIsActive(true);
                graphStoreService.saveRelationship(relationship);
                graphStoreService.saveRelationshipParagraphMapping(relationshipId, paragraphId, knowledgeId, documentId);
                result.add(relationship);
            } catch (Exception e) {
                log.warn("Failed to create embedding for relationship [{} -> {}]: {}", sourceName, targetName, e.getMessage());
                // Create relationship without embedding
                GraphRelationshipEntity relationship = new GraphRelationshipEntity();
                relationship.setId(relationshipId);
                relationship.setSourceEntityId(sourceEntity.getId());
                relationship.setTargetEntityId(targetEntity.getId());
                relationship.setDescription(description);
                relationship.setKeywords(keywords);
                relationship.setWeight(1.0);
                relationship.setKnowledgeId(knowledgeId);
                relationship.setDocumentId(documentId);
                relationship.setIsActive(true);
                graphStoreService.saveRelationship(relationship);
                graphStoreService.saveRelationshipParagraphMapping(relationshipId, paragraphId, knowledgeId, documentId);
                result.add(relationship);
            }
        }

        return result;
    }

    private void updateEntityDescription(GraphEntityEntity entity) {
        // Update entity description by using GraphStoreService's direct mapper update
        graphEntityMapper.update(
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<GraphEntityEntity>()
                        .set(GraphEntityEntity::getDescription, entity.getDescription())
                        .eq(GraphEntityEntity::getId, entity.getId())
        );
    }
}