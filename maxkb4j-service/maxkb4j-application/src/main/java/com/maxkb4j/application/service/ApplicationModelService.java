package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.enums.AppType;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.application.util.WorkFlowNodes;
import com.maxkb4j.model.enums.ModelType;
import com.maxkb4j.model.service.IModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.maxkb4j.workflow.enums.NodeType.*;

/**
 * 应用 MK 模板导入逻辑，从 {@link ApplicationServiceImpl} 抽离。
 * 负责从 classpath 加载并解析 .mk 模板，并在持久化前标准化应用与工具数据。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ApplicationModelService {


    /** 需要校验并替换 LLM 模型 ID 的工作流节点类型 */
    private static final Set<String> LLM_NODE_TYPES = Set.of(
            QUESTION.getKey(), NL2SQL.getKey(), INTENT_CLASSIFY.getKey(),
            IMAGE_UNDERSTAND.getKey(), AI_CHAT.getKey(), PARAMETER_EXTRACTION.getKey());

    private final IModelService modelService;


    public void normalizeAppModels(ApplicationEntity app) {
        if (AppType.SIMPLE.name().equals(app.getType())&& StringUtils.isNotBlank(app.getModelId())){
            app.setModelId(modelService.getSafeModelId(app.getModelId(), ModelType.LLM));
        }
        if (AppType.SIMPLE.name().equals(app.getType())&& StringUtils.isNotBlank(app.getTtsModelId())){
            app.setTtsModelId(modelService.getSafeModelId(app.getModelId(), ModelType.TTS));
        }
        if (AppType.SIMPLE.name().equals(app.getType())&& StringUtils.isNotBlank(app.getSttModelId())){
            app.setSttModelId(modelService.getSafeModelId(app.getModelId(), ModelType.STT));
        }
        normalizeNodeModels(app.getWorkFlow());
    }

    private void normalizeNodeModels(JSONObject workFlow) {
        JSONArray nodes = WorkFlowNodes.getNodes(workFlow);
        if (nodes == null) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (node == null) {
                continue;
            }
            String type = node.getString("type");
            if (BASE.getKey().equals(type)) {
                JSONObject nodeData = WorkFlowNodes.getNodeData(node);
                if (nodeData != null) {
                    String ttsModelId = nodeData.getString("ttsModelId");
                    if (ttsModelId != null){
                        nodeData.put("ttsModelId", modelService.getSafeModelId(ttsModelId, ModelType.TTS));
                    }
                    String sttModelId = nodeData.getString("sttModelId");
                    if (sttModelId != null){
                        nodeData.put("sttModelId", modelService.getSafeModelId(sttModelId, ModelType.STT));
                    }
                }
            }
            if (LLM_NODE_TYPES.contains(type)) {
                JSONObject nodeData = WorkFlowNodes.getNodeData(node);
                if (nodeData != null) {
                    String modelId = nodeData.getString("modelId");
                    if (modelId != null){
                        nodeData.put("modelId", modelService.getSafeModelId(modelId, ModelType.LLM));
                    }
                }
            }
            if (IMAGE_GENERATE.getKey().equals(type)) {
                JSONObject nodeData = WorkFlowNodes.getNodeData(node);
                if (nodeData != null) {
                    String modelId = nodeData.getString("modelId");
                    if (modelId != null){
                        nodeData.put("modelId", modelService.getSafeModelId(modelId, ModelType.TTI));
                    }
                }
            }
            if (TEXT_TO_SPEECH.getKey().equals(type)) {
                JSONObject nodeData = WorkFlowNodes.getNodeData(node);
                if (nodeData != null) {
                    String ttsModelId = nodeData.getString("ttsModelId");
                    if (ttsModelId != null){
                        nodeData.put("ttsModelId", modelService.getSafeModelId(ttsModelId, ModelType.TTS));
                    }
                }
            }
            if (SPEECH_TO_TEXT.getKey().equals(type)) {
                JSONObject nodeData = WorkFlowNodes.getNodeData(node);
                if (nodeData != null) {
                    String sttModelId = nodeData.getString("sttModelId");
                    if (sttModelId != null){
                        nodeData.put("sttModelId", modelService.getSafeModelId(sttModelId, ModelType.STT));
                    }
                }
            }
            if (LOOP.getKey().equals(type)){
                JSONObject nodeData = WorkFlowNodes.getNodeData(node);
                if (nodeData != null){
                    JSONObject loopBody= nodeData.getJSONObject("loopBody");
                    normalizeNodeModels(loopBody);
                }
            }
        }
    }
}
