package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.system.dto.TargetResource;
import com.maxkb4j.system.service.IResourceMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 应用资源映射（知识库/工具/模型）维护逻辑，从 {@link ApplicationServiceImpl} 抽离。
 * 负责从应用配置及工作流节点中收集资源 ID 并批量建立映射关系。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ApplicationResourceMappingService {

    private final IResourceMappingService resourceMappingService;

    /**
     * 收集应用自身及工作流节点中引用的知识库/工具/模型 ID，建立资源映射。
     */
    public void saveResourceMappings(ApplicationEntity app) {
        List<String> modelIds = new ArrayList<>(Stream.of(app.getModelId(), app.getSttModelId(), app.getTtsModelId())
                .filter(Objects::nonNull)
                .toList());
        List<String> knowledgeIds = app.getKnowledgeIds() == null ? new ArrayList<>() : new ArrayList<>(app.getKnowledgeIds());
        List<String> toolIds = app.getToolIds() == null ? new ArrayList<>() : new ArrayList<>(app.getToolIds());
        JSONObject workFlow = app.getWorkFlow();
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
                            JSONArray toolIdArray = nodeData.getJSONArray("toolIds");
                            toolIds.addAll(new ArrayList<>(toolIdArray.toJavaList(String.class)));
                        }
                        if (nodeData != null && nodeData.containsKey("knowledgeIds")) {
                            JSONArray knowledgeIdArray = nodeData.getJSONArray("knowledgeIds");
                            knowledgeIds.addAll(new ArrayList<>(knowledgeIdArray.toJavaList(String.class)));
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
        saveResourceMappings(app.getId(), knowledgeIds, toolIds, modelIds);
    }

    /**
     * 删除应用关联的全部资源映射。
     */
    public void deleteResourceMappings(String appId) {
        resourceMappingService.deleteBySourceId(ResourceType.APPLICATION, appId);
    }

    /**
     * 批量保存资源映射关系
     */
    private void saveResourceMappings(String appId,
                                      List<String> knowledgeIds,
                                      List<String> toolIds,
                                      List<String> modelIds) {
        knowledgeIds = knowledgeIds == null ? List.of() : knowledgeIds.stream().filter(Objects::nonNull).toList();
        List<TargetResource> targets = new ArrayList<>(knowledgeIds.stream().map(id -> new TargetResource(id, ResourceType.KNOWLEDGE)).toList());
        toolIds = toolIds == null ? List.of() : toolIds.stream().filter(Objects::nonNull).toList();
        targets.addAll(toolIds.stream().map(id -> new TargetResource(id, ResourceType.TOOL)).toList());
        targets.addAll(modelIds.stream().filter(Objects::nonNull).map(id -> new TargetResource(id, ResourceType.MODEL)).toList());
        resourceMappingService.relation(ResourceType.APPLICATION, appId, targets);
    }
}
