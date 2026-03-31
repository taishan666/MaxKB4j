package com.maxkb4j.workflow.exception;

import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 异常解析器链
 * 管理多个 NodeExceptionResolver 并按顺序执行
 */
@Slf4j
@Component
public class ExceptionResolverChain {

    private final List<NodeExceptionResolver> resolvers;

    public ExceptionResolverChain(List<NodeExceptionResolver> resolverList) {
        // 按 order 排序
        this.resolvers = new ArrayList<>(resolverList);
        this.resolvers.sort(Comparator.comparingInt(NodeExceptionResolver::getOrder));
        log.info("ExceptionResolverChain initialized with {} resolvers", resolvers.size());
    }

    /**
     * 执行异常解析链
     *
     * @param workflow 工作流上下文
     * @param node     发生异常的节点
     * @param ex       异常信息
     */
    public void resolve(Workflow workflow, AbsNode node, Exception ex) {
        for (NodeExceptionResolver resolver : resolvers) {
            try {
                boolean shouldContinue = resolver.resolve(workflow, node, ex);
                if (!shouldContinue) {
                    log.debug("Exception resolver chain terminated by {}", resolver.getClass().getSimpleName());
                    break;
                }
            } catch (Exception e) {
                log.warn("Exception resolver {} failed: {}", resolver.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * 获取解析器数量
     *
     * @return 解析器数量
     */
    public int size() {
        return resolvers.size();
    }
}