package com.maxkb4j.workflow.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.workflow.enums.NodeStatus;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.ModelAwareParams;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

/**
 * Base class for node handlers.
 *
 * <p>Subclasses implement {@link #doExecute} (synchronous) or override
 * {@link #doExecuteAsync} (streaming/async). The final {@link #execute} template wraps
 * them with the behavior shared by every handler: emitting the node-start message,
 * recording the execution time and propagating failures through the returned future.</p>
 */
@Slf4j
public abstract class AbsNodeHandler implements INodeHandler {

    /**
     * Core synchronous execution logic. Async subclasses should override
     * {@link #doExecuteAsync} instead of this method.
     *
     * @param workflow the workflow context
     * @param node     the node instance
     * @return the node result
     * @throws Exception execution failure
     */
    protected abstract NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception;

    /**
     * Core asynchronous execution logic. Defaults to wrapping {@link #doExecute} in a
     * completed future, so synchronous subclasses need no override.
     */
    protected CompletableFuture<NodeResult> doExecuteAsync(IWorkflow workflow, AbsNode node) throws Exception {
        return CompletableFuture.completedFuture(doExecute(workflow, node));
    }

    /**
     * Template method: emits the node-start message, runs the node, records the execution
     * time on success and propagates failures through the returned future.
     * Final - subclasses customize behavior through {@link #doExecute}/{@link #doExecuteAsync}.
     */
    @Override
    public final CompletableFuture<NodeResult> execute(IWorkflow workflow, AbsNode node) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            node.setStatus(NodeStatus.STARTED.getStatus());
            return doExecuteAsync(workflow, node)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            recordExecutionTime(node, startTime);
                        }
                    });
        } catch (Exception ex) {
            CompletableFuture<NodeResult> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }

    /**
     * Parses node params from {@code node.getNodeData()} into the given type.
     *
     * @param node        the node instance
     * @param paramsClass the params class
     * @return the parsed params, or null when the class or node data is missing
     */
    protected <P> P parseParams(AbsNode node, Class<P> paramsClass) {
        JSONObject nodeData = node.getNodeData();
        if (paramsClass == null) {
            log.warn("Cannot parse params: paramsClass is null for handler {}", this.getClass().getSimpleName());
            return null;
        }
        if (nodeData == null || nodeData.isEmpty()) {
            log.warn("Cannot parse params: nodeData is empty for node {}", node.getId());
            return null;
        }
        return nodeData.toJavaObject(paramsClass);
    }

    /**
     * Records the node execution time (seconds) into the node detail.
     */
    protected void recordExecutionTime(AbsNode node, long startTime) {
        long endTime = System.currentTimeMillis();
        float runTime = (endTime - startTime) / 1000F;
        node.getDetail().put(RuntimeDetailField.RUN_TIME, runTime);
        String nodeName = node.getProperties() != null ? node.getProperties().getString(RuntimeDetailField.NODE_NAME) : node.getType();
        log.info("node: {}, runTime: {} s", nodeName, runTime);
    }

    // ==================== detail helpers ====================

    protected void putDetail(AbsNode node, String key, Object value) {
        node.getDetail().put(key, value);
    }

    protected void putDetails(AbsNode node, Map<String, Object> details) {
        if (details != null) {
            node.getDetail().putAll(details);
        }
    }

    protected void setAnswerText(AbsNode node, String answer) {
        node.setAnswerText(answer);
    }

    /**
     * Reads the interrupt flag written by loop control nodes.
     */
    protected boolean getInterruptFlag(AbsNode node) {
        Object flag = node.getDetail().get(NodeField.IS_INTERRUPT_EXEC);
        return Boolean.TRUE.equals(flag);
    }

    // ==================== reference helpers ====================

    protected Object getReferenceField(IWorkflow workflow, List<String> fields) {
        return workflow.getReferenceField(fields);
    }

    protected String getReferenceFieldAsString(IWorkflow workflow, List<String> fields) {
        Object value = workflow.getReferenceField(fields);
        return value instanceof String ? (String) value : null;
    }

    protected List<OssFile> getOssFiles(IWorkflow workflow, List<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return List.of();
        }
        Object object = workflow.getReferenceField(fields);
        if (!(object instanceof List<?> fileList)) {
            return List.of();
        }
        return fileList.stream()
                .filter(OssFile.class::isInstance)
                .map(OssFile.class::cast)
                .toList();
    }

    // ==================== model configuration ====================

    /**
     * Resolves the model configuration for a node. When
     * {@link ModelAwareParams#getModelIdType()} is {@code VariableField.REFERENCE} the configuration
     * is read from the referenced workflow field; otherwise the params' own
     * modelId/modelParamsSetting are used.
     *
     * @param workflow the workflow context, used to resolve reference fields
     * @param params   the node params implementing {@link ModelAwareParams}
     * @return the resolved configuration, or null when params is null
     */
    protected ModelConfig resolveModelConfig(IWorkflow workflow, ModelAwareParams params) {
        if (params == null) {
            return null;
        }
        String modelId = params.getModelId();
        JSONObject modelParamsSetting = params.getModelParamsSetting();
        if (VariableField.REFERENCE.equals(params.getModelIdType())) {
            ModelConfig modelConfig = ModelConfig.from(workflow.getReferenceField(params.getModelIdReference()));
            if (modelConfig != null) {
                modelId = modelConfig.getModelId();
                modelParamsSetting = modelConfig.getModelParamsSetting();
            }
        }
        ModelConfig resolved = new ModelConfig();
        resolved.setModelId(modelId);
        resolved.setModelParamsSetting(modelParamsSetting);
        return resolved;
    }

    // ==================== token usage ====================

    /**
     * Writes token usage into the node detail; silently skips null usage.
     */
    protected void recordTokenUsage(AbsNode node, TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return;
        }
        putDetails(node, Map.of(
                NodeField.MESSAGE_TOKENS, tokenUsage.inputTokenCount(),
                NodeField.ANSWER_TOKENS, tokenUsage.outputTokenCount()
        ));
    }
}
