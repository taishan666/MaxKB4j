package com.maxkb4j.knowledge.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.knowledge.dto.KnowledgeQuery;
import com.maxkb4j.knowledge.dto.KnowledgeSimple;
import com.maxkb4j.knowledge.dto.WebKnowledgeDTO;
import com.maxkb4j.knowledge.entity.*;
import com.maxkb4j.knowledge.event.CreateWebDocsEvent;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.mapper.ProblemMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import com.maxkb4j.knowledge.service.IKnowledgeInternalService;
import com.maxkb4j.knowledge.service.ITagService;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.KnowledgeListVO;
import com.maxkb4j.knowledge.vo.KnowledgeVO;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.system.dto.TargetResource;
import com.maxkb4j.system.service.IResourceMappingService;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 知识库核心服务
 * 负责知识库的CRUD、权限映射、资源映射等
 *
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, KnowledgeEntity> implements IKnowledgeInternalService {

    private final ProblemMapper problemMapper;
    private final ParagraphMapper paragraphMapper;
    private final ProblemParagraphMapper problemParagraphMapper;
    private final DocumentServiceImpl documentService;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final DataPermissionSupport dataPermissionSupport;
    private final ApplicationEventPublisher eventPublisher;
    private final IDataStore compositeStore;
    private final KnowledgeActionServiceImpl knowledgeActionService;
    private final KnowledgeVersionServiceImpl knowledgeVersionService;
    private final IResourceMappingService resourceMappingService;
    private final ITagService tagService;
    private final UserContext userContext;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    /**
     * 批量删除分块大小：每个分块一个独立短事务并独立清理向量库，避免大批量删除时
     * 长事务持有连接/锁、或单条超大 IN/DELETE 触发数据库超时。
     */
    private static final int DELETE_CHUNK_SIZE = 200;

    @PostConstruct
    private void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


    @Override
    public IPage<KnowledgeVO> pageList(Page<KnowledgeVO> knowledgePage, KnowledgeQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.KNOWLEDGE);
        return baseMapper.pageList(knowledgePage, query);
    }


    public KnowledgeVO getKnowledgeById(String id) {
        KnowledgeEntity entity = baseMapper.selectById(id);
        if (Objects.isNull(entity)) {
            return null;
        }
        return BeanUtil.copy(entity, KnowledgeVO.class);
    }


    public List<ParagraphEntity> getParagraphByProblemId(String problemId) {
        List<ProblemParagraphEntity> list = problemParagraphMapper.selectList(Wrappers.<ProblemParagraphEntity>lambdaQuery().select(ProblemParagraphEntity::getParagraphId).eq(ProblemParagraphEntity::getProblemId, problemId));
        if (!CollectionUtils.isEmpty(list)) {
            List<String> paragraphIds = list.stream().map(ProblemParagraphEntity::getParagraphId).toList();
            return paragraphMapper.selectByIds(paragraphIds);
        }
        return Collections.emptyList();
    }


    public Boolean deleteById(String id) {
        return deleteKnowledge(List.of(id));
    }

    public Boolean delMulApplication(List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return false;
        }
        return deleteKnowledge(idList);
    }

    /**
     * 批量删除知识库（单删同样走此路径）：
     * <ul>
     *   <li>关系型清理按 {@link #DELETE_CHUNK_SIZE} 分块，每块一个独立短事务，避免长事务占连接/锁导致超时；</li>
     *   <li>向量/全文库清理在关系型事务提交后执行——派生数据不在关系型事务内，避免大 embedding 表 DELETE
     *       被裹进事务而触发数据库超时；失败仅记日志（主行已删，孤儿向量无害且删除幂等）。</li>
     * </ul>
     */
    private Boolean deleteKnowledge(List<String> idList) {
        boolean removed = false;
        for (List<String> chunk : CollUtil.split(idList, DELETE_CHUNK_SIZE)) {
            Boolean chunkRemoved = transactionTemplate.execute(status -> deleteKnowledgeRelational(chunk));
            removed |= Boolean.TRUE.equals(chunkRemoved);
            try {
                compositeStore.deleteByKnowledgeIds(chunk);
            } catch (Exception e) {
                log.error("Delete vector store for knowledge chunk failed, relational rows already removed. ids={}, error={}", chunk, e.getMessage(), e);
            }
        }
        return removed;
    }

    /**
     * 单块关系型清理：用 {@code IN (...)} 合并所有按 knowledgeId 过滤的删除，从一次一条降到一次一块。
     */
    private boolean deleteKnowledgeRelational(List<String> chunk) {
        problemParagraphMapper.delete(Wrappers.<ProblemParagraphEntity>lambdaQuery().in(ProblemParagraphEntity::getKnowledgeId, chunk));
        problemMapper.delete(Wrappers.<ProblemEntity>lambdaQuery().in(ProblemEntity::getKnowledgeId, chunk));
        paragraphMapper.delete(Wrappers.<ParagraphEntity>lambdaQuery().in(ParagraphEntity::getKnowledgeId, chunk));
        documentService.deleteByKnowledgeIds(chunk);
        knowledgeVersionService.lambdaUpdate().in(KnowledgeVersionEntity::getKnowledgeId, chunk).remove();
        knowledgeActionService.lambdaUpdate().in(KnowledgeActionEntity::getKnowledgeId, chunk).remove();
        userResourcePermissionService.remove(AuthTargetType.KNOWLEDGE, chunk);
        resourceMappingService.deleteBySourceIds(ResourceType.KNOWLEDGE, chunk);
        tagService.lambdaUpdate().in(TagEntity::getKnowledgeId, chunk).remove();
        return this.removeByIds(chunk);
    }


    @Transactional
    public KnowledgeEntity createKnowledge(KnowledgeEntity knowledge) {
        knowledge.setMeta(new JSONObject());
        knowledge.setUserId(userContext.getUserId());
        if (knowledge.getWorkFlow() == null) {
            knowledge.setWorkFlow(new JSONObject());
        }
        this.save(knowledge);
        userResourcePermissionService.ownerSave(AuthTargetType.KNOWLEDGE, knowledge.getId(), knowledge.getUserId());
        saveResourceMappings(knowledge);
        return knowledge;
    }

    @Transactional
    public KnowledgeEntity createKnowledgeWeb(WebKnowledgeDTO knowledge) {
        createKnowledge(knowledge);
        // 使用事件驱动异步处理，确保事务提交后再执行
        eventPublisher.publishEvent(new CreateWebDocsEvent(
            this,
            knowledge.getId(),
            knowledge.getSourceUrl(),
            knowledge.getSelector()
        ));
        return knowledge;
    }


    public List<KnowledgeListVO> listKnowledge(KnowledgeQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.KNOWLEDGE);
        return baseMapper.listKnowledge(query);
    }

    @Override
    public List<KnowledgeSimple> listSimpleKnowledgeByIds(List<String> knowledgeIds) {
        List<KnowledgeEntity> list = this.lambdaQuery()
                .select(KnowledgeEntity::getId, KnowledgeEntity::getName, KnowledgeEntity::getDesc, KnowledgeEntity::getType)
                .in(KnowledgeEntity::getId, knowledgeIds)
                .orderByDesc(KnowledgeEntity::getCreateTime)
                .list();
        return BeanUtil.copyList(list, KnowledgeSimple.class);
    }


    public void saveResourceMappings(KnowledgeEntity knowledge) {
        List<String> modelIds = new ArrayList<>();
        modelIds.add(knowledge.getEmbeddingModelId());
        List<String> toolIds = new ArrayList<>();
        JSONObject workFlow = knowledge.getWorkFlow();
        if (workFlow != null && workFlow.containsKey("nodes")) {
            JSONArray nodes = workFlow.getJSONArray("nodes");
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    JSONObject properties = node.getJSONObject("properties");
                    if (properties != null && properties.containsKey("nodeData")) {
                        JSONObject nodeData = properties.getJSONObject("nodeData");
                        if (nodeData != null && nodeData.containsKey("toolLibId")) {
                            toolIds.add(nodeData.getString("toolLibId"));
                        }
                        if (nodeData != null && nodeData.containsKey("mcpToolId")) {
                            toolIds.add(nodeData.getString("mcpToolId"));
                        }
                        if (nodeData != null && nodeData.containsKey("toolIds")) {
                            toolIds.addAll((Collection<? extends String>) nodeData.get("toolIds"));
                        }
                        if (nodeData != null && nodeData.containsKey("modelId")) {
                            modelIds.add(nodeData.getString("modelId"));
                        }
                        if (nodeData != null && nodeData.containsKey("ttsModelId")) {
                            modelIds.add(nodeData.getString("ttsModelId"));
                        }
                        if (nodeData != null && nodeData.containsKey("sttModelId")) {
                            modelIds.add(nodeData.getString("sttModelId"));
                        }
                        if (nodeData != null && nodeData.containsKey("rerankerModelId")) {
                            modelIds.add(nodeData.getString("rerankerModelId"));
                        }
                    }
                }
            }
        }
        List<TargetResource> targets = new ArrayList<>();
        targets.addAll(toolIds.stream().map(id -> new TargetResource(id, ResourceType.TOOL)).toList());
        targets.addAll(modelIds.stream().filter(Objects::nonNull).map(id -> new TargetResource(id, ResourceType.MODEL)).toList());
        resourceMappingService.relation(ResourceType.KNOWLEDGE, knowledge.getId(), targets);
    }

    /**
     * 更新知识库并同步保存资源映射关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledge(String id, KnowledgeEntity knowledge) {
        knowledge.setId(id);
        this.updateById(knowledge);
        this.saveResourceMappings(knowledge);
    }
}
