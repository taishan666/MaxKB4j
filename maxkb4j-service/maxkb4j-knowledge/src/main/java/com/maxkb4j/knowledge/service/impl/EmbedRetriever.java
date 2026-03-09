package com.maxkb4j.knowledge.service.impl;

import com.maxkb4j.knowledge.consts.SearchType;
import com.maxkb4j.knowledge.mapper.EmbeddingMapper;
import com.maxkb4j.knowledge.service.IDataRetriever;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component(SearchType.EMBEDDING)
@RequiredArgsConstructor
public class EmbedRetriever implements IDataRetriever {

    private final KnowledgeModelService knowledgeModelService;
    private final EmbeddingMapper embeddingMapper;

    @Override
    public List<TextChunkVO> search(List<String> knowledgeIds, List<String> excludeParagraphIds, String keyword, int maxResults, float minScore) {
        if (StringUtils.isNotBlank(keyword)) {
            EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(knowledgeIds.get(0));
            try {
                Response<Embedding> res = embeddingModel.embed(keyword);
                return embeddingMapper.embeddingSearch(knowledgeIds, excludeParagraphIds, maxResults, minScore, res.content().vector(),embeddingModel.dimension());
            }catch (Exception e){
                log.error("向量化异常: {}", e.getMessage());
            }
        }
        return List.of();
    }
}
