package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.model.form.LocalFileUpload;
import com.maxkb4j.model.form.TextInputField;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.entity.*;
import com.maxkb4j.knowledge.event.GenerateProblemEvent;
import com.maxkb4j.user.service.IUserService;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.KnowledgeParams;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.IWorkFlowActuator;
import com.maxkb4j.workflow.service.WorkflowFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.maxkb4j.workflow.enums.NodeType.DATA_SOURCE_WEB;

/**
 * 知识库工作流服务
 * 负责工作流执行、数据源表单、向量化、问题生成等
 *
 * @author tarzan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeWorkflowService {

    private final IKnowledgeInternalService knowledgeService;
    private final IDocumentInternalService documentService;
    private final IKnowledgeVersionService knowledgeVersionService;
    private final IKnowledgeActionInternalService knowledgeActionService;
    private final IWorkFlowActuator workFlowActuator;
    private final NodeBuilder nodeBuilder;
    private final WorkflowFactory workflowFactory;
    private final UserContext userContext;
    private final IUserService userService;
    private final TaskExecutor workflowTaskExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final IProblemService problemService;

    /**
     * 数据源表单列表
     */
    public List<BaseField> datasourceFormList(String nodeType, JSONObject params) {
        if (DATA_SOURCE_WEB.getKey().equals(nodeType)) {
            BaseField field1 = new TextInputField("Web 根地址", "sourceUrl", "请输入 Web 根地址", true);
            BaseField field2 = new TextInputField("选择器", "selector", "默认为 body，可输入 .classname/#idname/tagname", false);
            return List.of(field1, field2);
        } else {
            BaseField localFileUpload = new LocalFileUpload(50, 100, List.of("TXT", "DOCX", "PDF", "HTML", "XLS", "XLSX", "CSV"));
            if (params == null) {
                return List.of(localFileUpload);
            }
            JSONObject node = params.getJSONObject("node");
            if (node == null) {
                return List.of(localFileUpload);
            }
            JSONObject properties = node.getJSONObject("properties");
            if (properties == null) {
                return List.of(localFileUpload);
            }
            JSONObject nodeData = properties.getJSONObject("nodeData");
            if (nodeData == null) {
                return List.of(localFileUpload);
            }
            Integer fileCountLimit = nodeData.getInteger("fileCountLimit");
            Integer fileSizeLimit = nodeData.getInteger("fileSizeLimit");
            List<String> fileTypeList = nodeData.getJSONArray("fileTypeList").toJavaList(String.class);
            return List.of(new LocalFileUpload(fileCountLimit, fileSizeLimit, fileTypeList));
        }
    }

    /**
     * 获取知识库工作流
     */
    public JSONObject getKnowledgeWorkFlow(String id, boolean debug) {
        JSONObject workFlow = null;
        if (debug) {
            KnowledgeEntity knowledge = knowledgeService.getById(id);
            if (knowledge != null) {
                workFlow = knowledge.getWorkFlow();
            }
        } else {
            KnowledgeVersionEntity knowledgeVersion = knowledgeVersionService.lambdaQuery()
                    .eq(KnowledgeVersionEntity::getKnowledgeId, id)
                    .orderByDesc(KnowledgeVersionEntity::getCreateTime)
                    .last("limit 1").one();
            if (knowledgeVersion != null) {
                workFlow = knowledgeVersion.getWorkFlow();
            }
        }
        return workFlow;
    }

    /**
     * 上传文档并执行工作流
     */
    public KnowledgeActionEntity uploadDocument(String id, KnowledgeParams params, boolean debug) {
        JSONObject knowledgeWorkFlow = getKnowledgeWorkFlow(id, debug);
        if (knowledgeWorkFlow == null) {
            throw new IllegalArgumentException("未找到知识库 ID: " + id);
        }
        KnowledgeActionEntity knowledgeAction = new KnowledgeActionEntity();
        knowledgeAction.setKnowledgeId(id);
        knowledgeAction.setState("STARTED");
        knowledgeAction.setDetails(new JSONObject());
        knowledgeAction.setRunTime(0F);
        JSONObject meta = new JSONObject();
        String userId = userContext.getUserId();
        meta.put("userId", userId);
        meta.put("username", userService.getUsername(userId));
        knowledgeAction.setMeta(meta);
        knowledgeActionService.save(knowledgeAction);
        LogicFlow logicFlow = LogicFlow.newInstance(knowledgeWorkFlow);
        List<AbsNode> nodes = logicFlow.getNodes().stream().map(nodeBuilder::getNode).filter(Objects::nonNull).toList();
        params.setActionId(knowledgeAction.getId());
        params.setKnowledgeId(id);
        params.setDebug(debug);
        IWorkflow workflow = workflowFactory.createKnowledge(nodes, logicFlow.getEdges(), params);
        CompletableFuture.runAsync(() -> workFlowActuator.execute(workflow), workflowTaskExecutor);
        return knowledgeAction;
    }

    /**
     * 更新知识库工作流
     */
    public KnowledgeEntity updateDatasetWorkflow(String id, KnowledgeEntity dataset) {
        dataset.setId(id);
        return knowledgeService.updateById(dataset) ? dataset : null;
    }

    /**
     * 知识库向量化
     */
    public boolean embeddingKnowledge(String knowledgeId) {
        List<DocumentEntity> documents = documentService.lambdaQuery()
                .select(DocumentEntity::getId)
                .eq(DocumentEntity::getKnowledgeId, knowledgeId).list();
        documentService.embedByDocIds(knowledgeId, documents.stream().map(DocumentEntity::getId).toList(), List.of("0", "1", "2", "3", "n"));
        List<ProblemEntity> problems = problemService.lambdaQuery()
                .select(ProblemEntity::getId)
                .eq(ProblemEntity::getKnowledgeId, knowledgeId).list();
        problemService.reIndexBatch(knowledgeId,problems.stream().map(ProblemEntity::getId).toList());
        return true;
    }

    /**
     * 生成相关问题
     */
    public Boolean generateRelated(String knowledgeId, GenerateProblemDTO dto) {
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId, dto.getDocumentIdList(), dto.getModelId(),dto.getModelParamsSetting(), dto.getNumber(), dto.getStateList()));
        return true;
    }
}
