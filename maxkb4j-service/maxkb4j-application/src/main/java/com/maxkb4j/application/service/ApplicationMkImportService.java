package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.MaxKb4J;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.enums.AppType;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.application.util.ResourceUtil;
import com.maxkb4j.application.util.WorkFlowNodes;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.IModelService;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.service.IToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

import static com.maxkb4j.workflow.enums.NodeType.AI_CHAT;
import static com.maxkb4j.workflow.enums.NodeType.IMAGE_UNDERSTAND;
import static com.maxkb4j.workflow.enums.NodeType.INTENT_CLASSIFY;
import static com.maxkb4j.workflow.enums.NodeType.NL2SQL;
import static com.maxkb4j.workflow.enums.NodeType.PARAMETER_EXTRACTION;
import static com.maxkb4j.workflow.enums.NodeType.QUESTION;

/**
 * 应用 MK 模板导入逻辑，从 {@link ApplicationServiceImpl} 抽离。
 * 负责从 classpath 加载并解析 .mk 模板，并在持久化前标准化应用与工具数据。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ApplicationMkImportService {

    private static final String TEMPLATE_LOCATION_PREFIX = "templates/app/";

    /** 需要校验并替换 LLM 模型 ID 的工作流节点类型 */
    private static final Set<String> LLM_NODE_TYPES = Set.of(
            QUESTION.getKey(), NL2SQL.getKey(), INTENT_CLASSIFY.getKey(),
            IMAGE_UNDERSTAND.getKey(), AI_CHAT.getKey(), PARAMETER_EXTRACTION.getKey());

    private final UserContext userContext;
    private final IToolService toolService;
    private final IModelService modelService;

    /**
     * 从 classpath 的 templates/app 目录加载并解析 .mk 模板。
     */
    public MaxKb4J loadClasspathTemplate(String downloadUrl) {
        String templatePath = normalizeTemplatePath(downloadUrl);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(TEMPLATE_LOCATION_PREFIX + templatePath);
        return ResourceUtil.parseMk(resource);
    }

    /**
     * 校验并规范化模板路径，防止通过 ../ 等手段逃逸出 templates/app 目录读取任意 classpath 资源。
     */
    private static String normalizeTemplatePath(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        String normalized;
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(downloadUrl).normalize();
            if (path.isAbsolute()) {
                throw new IllegalArgumentException("非法的模板名称: " + downloadUrl);
            }
            normalized = path.toString().replace('\\', '/');
        } catch (java.nio.file.InvalidPathException e) {
            throw new IllegalArgumentException("非法的模板名称: " + downloadUrl);
        }
        if (normalized.startsWith("..") || normalized.contains("/../") || normalized.endsWith("/..")) {
            throw new IllegalArgumentException("非法的模板名称: " + downloadUrl);
        }
        if (!normalized.endsWith(".mk")) {
            throw new IllegalArgumentException("模板必须为 .mk 文件: " + downloadUrl);
        }
        return normalized;
    }

    /**
     * 导入前标准化应用与工具数据：
     * <ul>
     *     <li>重置发布状态并记录归属用户</li>
     *     <li>清空审计字段交由 MyBatis-Plus 自动填充，避免更新已有应用时覆盖原 create_time</li>
     *     <li>校验应用模型 ID，持久化工具并回填 toolIds</li>
     *     <li>将工作流中 LLM 节点的模型 ID 替换为当前可用模型</li>
     * </ul>
     */
    public void normalizeForImport(ApplicationEntity app, List<ToolDTO> toolList) {
        String userId = userContext.getUserId();
        app.setIsPublish(false);
        app.setUserId(userId);
        app.setCreateTime(null);
        app.setUpdateTime(null);
        if (AppType.SIMPLE.name().equals(app.getType())){
            app.setModelId(modelService.getSafeModelId(app.getModelId(), ModelType.LLM));
        }
        if (!CollectionUtils.isEmpty(toolList)) {
            toolService.saveOrUpdateBatch(toolList,userId);
            if (AppType.SIMPLE.name().equals(app.getType())){
                app.getToolIds().addAll(toolList.stream().map(ToolDTO::getId).toList());
            }
        }
        normalizeLlmNodeModels(app.getWorkFlow());
    }

    private void normalizeLlmNodeModels(JSONObject workFlow) {
        JSONArray nodes = WorkFlowNodes.getNodes(workFlow);
        if (nodes == null) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (node == null || !LLM_NODE_TYPES.contains(node.getString("type"))) {
                continue;
            }
            JSONObject nodeData = WorkFlowNodes.getNodeData(node);
            if (nodeData != null) {
                nodeData.put("modelId", modelService.getSafeModelId(nodeData.getString("modelId"), ModelType.LLM));
            }
        }
    }
}
