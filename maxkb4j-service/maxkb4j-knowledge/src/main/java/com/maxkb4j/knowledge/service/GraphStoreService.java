package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.GraphEntityEntity;
import com.maxkb4j.knowledge.entity.GraphEntityParagraphMappingEntity;
import com.maxkb4j.knowledge.entity.GraphRelationshipEntity;
import com.maxkb4j.knowledge.entity.GraphRelationshipParagraphMappingEntity;
import com.maxkb4j.knowledge.mapper.GraphEntityMapper;
import com.maxkb4j.knowledge.mapper.GraphEntityParagraphMappingMapper;
import com.maxkb4j.knowledge.mapper.GraphRelationshipMapper;
import com.maxkb4j.knowledge.mapper.GraphRelationshipParagraphMappingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphStoreService {

    private final GraphEntityMapper graphEntityMapper;
    private final GraphRelationshipMapper graphRelationshipMapper;
    private final GraphEntityParagraphMappingMapper entityParagraphMapper;
    private final GraphRelationshipParagraphMappingMapper relationshipParagraphMapper;

    public void saveEntity(GraphEntityEntity entity) {
        graphEntityMapper.insert(entity);
    }

    public void batchSaveEntities(List<GraphEntityEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) return;
        entities.forEach(e -> graphEntityMapper.insert(e));
    }

    public void saveRelationship(GraphRelationshipEntity relationship) {
        graphRelationshipMapper.insert(relationship);
    }

    public void batchSaveRelationships(List<GraphRelationshipEntity> relationships) {
        if (CollectionUtils.isEmpty(relationships)) return;
        relationships.forEach(r -> graphRelationshipMapper.insert(r));
    }

    public void saveEntityParagraphMapping(String entityId, String paragraphId, String knowledgeId, String documentId) {
        GraphEntityParagraphMappingEntity mapping = new GraphEntityParagraphMappingEntity();
        mapping.setId(IdWorker.get32UUID());
        mapping.setEntityId(entityId);
        mapping.setParagraphId(paragraphId);
        mapping.setKnowledgeId(knowledgeId);
        mapping.setDocumentId(documentId);
        entityParagraphMapper.insert(mapping);
    }

    public void batchSaveEntityParagraphMappings(List<GraphEntityParagraphMappingEntity> mappings) {
        if (CollectionUtils.isEmpty(mappings)) return;
        mappings.forEach(m -> entityParagraphMapper.insert(m));
    }

    public void saveRelationshipParagraphMapping(String relationshipId, String paragraphId, String knowledgeId, String documentId) {
        GraphRelationshipParagraphMappingEntity mapping = new GraphRelationshipParagraphMappingEntity();
        mapping.setId(IdWorker.get32UUID());
        mapping.setRelationshipId(relationshipId);
        mapping.setParagraphId(paragraphId);
        mapping.setKnowledgeId(knowledgeId);
        mapping.setDocumentId(documentId);
        relationshipParagraphMapper.insert(mapping);
    }

    public void batchSaveRelationshipParagraphMappings(List<GraphRelationshipParagraphMappingEntity> mappings) {
        if (CollectionUtils.isEmpty(mappings)) return;
        mappings.forEach(m -> relationshipParagraphMapper.insert(m));
    }

    public GraphEntityEntity findEntityByName(String knowledgeId, String name) {
        return graphEntityMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityEntity>()
                        .eq(GraphEntityEntity::getKnowledgeId, knowledgeId)
                        .eq(GraphEntityEntity::getName, name)
                        .eq(GraphEntityEntity::getIsActive, true)
        );
    }

    public List<GraphEntityEntity> findEntitiesByNames(String knowledgeId, List<String> names) {
        if (CollectionUtils.isEmpty(names)) return Collections.emptyList();
        return graphEntityMapper.searchByNames(knowledgeId, names);
    }

    public List<GraphEntityEntity> findEntitiesByNameLike(String knowledgeId, String keyword) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        return graphEntityMapper.searchByNameLike(knowledgeId, keyword);
    }

    public List<GraphRelationshipEntity> findRelationshipsByKeywords(String knowledgeId, List<String> keywords) {
        if (CollectionUtils.isEmpty(keywords)) return Collections.emptyList();
        return graphRelationshipMapper.searchByKeywords(knowledgeId, keywords);
    }

    public List<GraphRelationshipEntity> getNeighborRelationships(String knowledgeId, List<String> entityIds) {
        if (CollectionUtils.isEmpty(entityIds)) return Collections.emptyList();
        return graphRelationshipMapper.searchByEntityIds(knowledgeId, entityIds);
    }

    public List<String> getParagraphIdsByEntityIds(List<String> entityIds, String knowledgeId) {
        if (CollectionUtils.isEmpty(entityIds)) return Collections.emptyList();
        return entityParagraphMapper.getParagraphIdsByEntityIdsAndKnowledgeId(entityIds, knowledgeId);
    }

    public List<String> getParagraphIdsByRelationshipIds(List<String> relationshipIds, String knowledgeId) {
        if (CollectionUtils.isEmpty(relationshipIds)) return Collections.emptyList();
        return relationshipParagraphMapper.getParagraphIdsByRelationshipIdsAndKnowledgeId(relationshipIds, knowledgeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) return;
        // Delete entity-paragraph mappings
        entityParagraphMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityParagraphMappingEntity>()
                        .eq(GraphEntityParagraphMappingEntity::getKnowledgeId, knowledgeId)
                        .in(GraphEntityParagraphMappingEntity::getDocumentId, documentIds)
        );
        // Delete relationship-paragraph mappings
        relationshipParagraphMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipParagraphMappingEntity>()
                        .eq(GraphRelationshipParagraphMappingEntity::getKnowledgeId, knowledgeId)
                        .in(GraphRelationshipParagraphMappingEntity::getDocumentId, documentIds)
        );
        // Delete relationships
        graphRelationshipMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipEntity>()
                        .eq(GraphRelationshipEntity::getKnowledgeId, knowledgeId)
                        .in(GraphRelationshipEntity::getDocumentId, documentIds)
        );
        // Delete entities
        graphEntityMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityEntity>()
                        .eq(GraphEntityEntity::getKnowledgeId, knowledgeId)
                        .in(GraphEntityEntity::getDocumentId, documentIds)
        );
        log.debug("Deleted graph data for documents: {}", documentIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) return;
        entityParagraphMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityParagraphMappingEntity>()
                        .eq(GraphEntityParagraphMappingEntity::getKnowledgeId, knowledgeId)
        );
        relationshipParagraphMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipParagraphMappingEntity>()
                        .eq(GraphRelationshipParagraphMappingEntity::getKnowledgeId, knowledgeId)
        );
        graphRelationshipMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipEntity>()
                        .eq(GraphRelationshipEntity::getKnowledgeId, knowledgeId)
        );
        graphEntityMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityEntity>()
                        .eq(GraphEntityEntity::getKnowledgeId, knowledgeId)
        );
        log.debug("Deleted all graph data for knowledge: {}", knowledgeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (CollectionUtils.isEmpty(paragraphIds)) return;
        // Delete entity-paragraph mappings for these paragraphs
        entityParagraphMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityParagraphMappingEntity>()
                        .eq(GraphEntityParagraphMappingEntity::getKnowledgeId, knowledgeId)
                        .in(GraphEntityParagraphMappingEntity::getParagraphId, paragraphIds)
        );
        // Delete relationship-paragraph mappings for these paragraphs
        relationshipParagraphMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipParagraphMappingEntity>()
                        .eq(GraphRelationshipParagraphMappingEntity::getKnowledgeId, knowledgeId)
                        .in(GraphRelationshipParagraphMappingEntity::getParagraphId, paragraphIds)
        );
        // Clean up orphan entities (no remaining paragraph mappings)
        cleanupOrphanEntities(knowledgeId);
        // Clean up orphan relationships (source/target entities deleted, or no paragraph mappings)
        cleanupOrphanRelationships(knowledgeId);
        log.debug("Deleted graph mappings for paragraphs: {}", paragraphIds);
    }

    private void cleanupOrphanEntities(String knowledgeId) {
        // Find entities that have no paragraph mappings left
        List<GraphEntityEntity> allEntities = graphEntityMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityEntity>()
                        .eq(GraphEntityEntity::getKnowledgeId, knowledgeId)
        );
        List<String> orphanEntityIds = new ArrayList<>();
        for (GraphEntityEntity entity : allEntities) {
            Long count = entityParagraphMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityParagraphMappingEntity>()
                            .eq(GraphEntityParagraphMappingEntity::getEntityId, entity.getId())
            );
            if (count == 0) {
                orphanEntityIds.add(entity.getId());
            }
        }
        if (!orphanEntityIds.isEmpty()) {
            graphEntityMapper.deleteBatchIds(orphanEntityIds);
            log.debug("Cleaned up {} orphan entities", orphanEntityIds.size());
        }
    }

    private void cleanupOrphanRelationships(String knowledgeId) {
        // Find relationships that have no paragraph mappings left or whose source/target entities no longer exist
        List<GraphRelationshipEntity> allRelationships = graphRelationshipMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipEntity>()
                        .eq(GraphRelationshipEntity::getKnowledgeId, knowledgeId)
        );
        List<String> orphanRelationshipIds = new ArrayList<>();
        for (GraphRelationshipEntity rel : allRelationships) {
            Long mappingCount = relationshipParagraphMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipParagraphMappingEntity>()
                            .eq(GraphRelationshipParagraphMappingEntity::getRelationshipId, rel.getId())
            );
            if (mappingCount == 0) {
                orphanRelationshipIds.add(rel.getId());
            }
        }
        if (!orphanRelationshipIds.isEmpty()) {
            graphRelationshipMapper.deleteBatchIds(orphanRelationshipIds);
            log.debug("Cleaned up {} orphan relationships", orphanRelationshipIds.size());
        }
    }

    /**
     * Get entity IDs connected to a specific paragraph
     */
    public List<String> getEntityIdsByParagraphId(String knowledgeId, String paragraphId) {
        List<GraphEntityParagraphMappingEntity> mappings = entityParagraphMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphEntityParagraphMappingEntity>()
                        .eq(GraphEntityParagraphMappingEntity::getKnowledgeId, knowledgeId)
                        .eq(GraphEntityParagraphMappingEntity::getParagraphId, paragraphId)
        );
        return mappings.stream().map(GraphEntityParagraphMappingEntity::getEntityId).toList();
    }

    /**
     * Get relationship IDs connected to a specific paragraph
     */
    public List<String> getRelationshipIdsByParagraphId(String knowledgeId, String paragraphId) {
        List<GraphRelationshipParagraphMappingEntity> mappings = relationshipParagraphMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GraphRelationshipParagraphMappingEntity>()
                        .eq(GraphRelationshipParagraphMappingEntity::getKnowledgeId, knowledgeId)
                        .eq(GraphRelationshipParagraphMappingEntity::getParagraphId, paragraphId)
        );
        return mappings.stream().map(GraphRelationshipParagraphMappingEntity::getRelationshipId).toList();
    }
}