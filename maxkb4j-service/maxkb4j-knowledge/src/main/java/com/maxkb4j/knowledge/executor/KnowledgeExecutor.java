package com.maxkb4j.knowledge.executor;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.core.support.RagContentInjector;
import com.maxkb4j.knowledge.service.IRetrieveService;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.tool.executor.AbsToolExecutor;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class KnowledgeExecutor extends AbsToolExecutor {

    private final String knowledgeId;
    private final KnowledgeSetting knowledgeSetting;
    private final IRetrieveService retrieveService;
    public  final RagContentInjector contentInjector;

    public KnowledgeExecutor(String knowledgeId,KnowledgeSetting knowledgeSetting, IRetrieveService retrieveService) {
        this.knowledgeId = knowledgeId;
        this.knowledgeSetting = knowledgeSetting;
        this.retrieveService = retrieveService;
        this.contentInjector = new RagContentInjector();

    }


    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Map<String, Object> args = argumentsAsMap(toolExecutionRequest.arguments());
        @SuppressWarnings("unchecked")
        List<String> queries = (List<String>) args.getOrDefault("queries", List.of());

        int maxCharNumber = knowledgeSetting.getMaxParagraphCharNumber();

        // 并行检索每个 query
        List<CompletableFuture<List<ParagraphRagVO>>> futures = queries.stream()
                .map(query -> CompletableFuture.supplyAsync(() ->
                        retrieveService.paragraphSearch(
                                query,
                                List.of(knowledgeId),
                                List.of(),
                                knowledgeSetting)))
                .toList();
        // 等待所有任务完成并合并结果
        Set<String> seenIds = new HashSet<>();
        List<ParagraphRagVO> paragraphList = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .filter(p -> seenIds.add(p.getId()))
                .toList();
        return contentInjector.format(paragraphList, maxCharNumber);
    }

}

