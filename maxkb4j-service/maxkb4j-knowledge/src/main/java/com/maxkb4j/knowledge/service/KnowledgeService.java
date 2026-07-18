package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.core.event.CreateWebDocsEvent;
import com.maxkb4j.knowledge.dto.KnowledgeQuery;
import com.maxkb4j.knowledge.dto.WebKnowledgeDTO;
import com.maxkb4j.knowledge.entity.*;
import com.maxkb4j.knowledge.mapper.KnowledgeMapper;
import com.maxkb4j.knowledge.mapper.ParagraphMapper;
import com.maxkb4j.knowledge.mapper.ProblemMapper;
import com.maxkb4j.knowledge.mapper.ProblemParagraphMapper;
import com.maxkb4j.knowledge.store.IDataStore;
import com.maxkb4j.knowledge.vo.KnowledgeListVO;
import com.maxkb4j.knowledge.vo.KnowledgeVO;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.system.entity.TargetResource;
import com.maxkb4j.system.service.IResourceMappingService;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
public class KnowledgeService extends ServiceImpl<KnowledgeMapper, KnowledgeEntity> implements IKnowledgeService {

    private final ProblemMapper problemMapper;
    private final ParagraphMapper paragraphMapper;
    private final ProblemParagraphMapper problemParagraphMapper;
    private final DocumentService documentService;
    private final IUserService userService;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final ApplicationEventPublisher eventPublisher;
    private final IDataStore compositeStore;
    private final KnowledgeActionService knowledgeActionService;
    private final KnowledgeVersionService knowledgeVersionService;
    private final IResourceMappingService resourceMappingService;
    private final ITagService tagService;
    private final UserContext userContext;


    public IPage<KnowledgeVO> selectKnowledgePage(Page<KnowledgeVO> knowledgePage, KnowledgeQuery query) {
        String loginId = userContext.getUserId();
        List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.KNOWLEDGE, loginId);
        Set<String> role = userService.getRoleById(loginId);
        query.setIsAdmin(role.contains(RoleType.ADMIN));
        query.setTargetIds(targetIds);
        IPage<KnowledgeVO> page = baseMapper.selectKnowledgePage(knowledgePage, query);
        Map<String, String> nicknameMap = userService.getNicknameMap();
        page.getRecords().forEach(vo -> vo.setNickname(nicknameMap.get(vo.getUserId())));
        return page;
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


    @Transactional
    public Boolean deleteById(String id) {
        problemParagraphMapper.delete(Wrappers.<ProblemParagraphEntity>lambdaQuery().eq(ProblemParagraphEntity::getKnowledgeId, id));
        problemMapper.delete(Wrappers.<ProblemEntity>lambdaQuery().eq(ProblemEntity::getKnowledgeId, id));
        paragraphMapper.delete(Wrappers.<ParagraphEntity>lambdaQuery().eq(ParagraphEntity::getKnowledgeId, id));
        documentService.deleteByKnowledgeId(id);
        knowledgeVersionService.lambdaUpdate().eq(KnowledgeVersionEntity::getKnowledgeId, id).remove();
        knowledgeActionService.lambdaUpdate().eq(KnowledgeActionEntity::getKnowledgeId, id).remove();
        userResourcePermissionService.remove(AuthTargetType.KNOWLEDGE, id);
        compositeStore.deleteByKnowledgeId(id);
        resourceMappingService.deleteBySourceId(ResourceType.KNOWLEDGE, id);
        tagService.lambdaUpdate().eq(TagEntity::getKnowledgeId, id).remove();
        return this.removeById(id);
    }


    public List<KnowledgeEntity> list(String userId, String folderId) {
        return this.lambdaQuery().eq(KnowledgeEntity::getUserId, userId).eq(KnowledgeEntity::getFolderId, folderId).list();
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


    public List<KnowledgeListVO> listKnowledge() {
        String userId = userContext.getUserId();
        Set<String> role = userService.getRoleById(userId);
        List<KnowledgeEntity> list;
        if (role.contains(RoleType.ADMIN)) {
            list = this.lambdaQuery().select(KnowledgeEntity::getId, KnowledgeEntity::getName, KnowledgeEntity::getDesc, KnowledgeEntity::getType, KnowledgeEntity::getFolderId).orderByDesc(KnowledgeEntity::getCreateTime).list();
        } else {
            List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.KNOWLEDGE, userId);
            if (targetIds.isEmpty()) {
                return Collections.emptyList();
            }
            list = this.lambdaQuery().select(KnowledgeEntity::getId, KnowledgeEntity::getName, KnowledgeEntity::getDesc, KnowledgeEntity::getType, KnowledgeEntity::getFolderId).in(KnowledgeEntity::getId, targetIds).orderByDesc(KnowledgeEntity::getCreateTime).list();
        }
        return BeanUtil.copyList(list, KnowledgeListVO.class);
    }

    @Override
    public List<KnowledgeEntity> listNameAndDescByIds(List<String> knowledgeIds) {
        return this.lambdaQuery().select(KnowledgeEntity::getId, KnowledgeEntity::getName, KnowledgeEntity::getDesc).in(KnowledgeEntity::getId, knowledgeIds).list();
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

    public Boolean delMulApplication(List<String> idList) {
        Boolean result = false;
        for (String id : idList) {
            result = deleteById(id);
        }
        return result;
    }

    /**
     * 更新知识库并同步保存资源映射关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateKnowledge(String id, KnowledgeEntity knowledge) {
        knowledge.setId(id);
        this.updateById(knowledge);
        this.saveResourceMappings(knowledge);
        return true;
    }
}
