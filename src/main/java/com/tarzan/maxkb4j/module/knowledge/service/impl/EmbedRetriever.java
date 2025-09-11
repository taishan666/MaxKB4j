package com.tarzan.maxkb4j.module.knowledge.service.impl;

import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextChunkVO;
import com.tarzan.maxkb4j.module.knowledge.consts.SearchType;
import com.tarzan.maxkb4j.module.knowledge.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.knowledge.service.DatasetBaseService;
import com.tarzan.maxkb4j.module.knowledge.service.IDataRetriever;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component(SearchType.EMBEDDING)
@AllArgsConstructor
public class EmbedRetriever implements IDataRetriever {

    private final DatasetBaseService datasetService;
    private final EmbeddingMapper embeddingMapper;

    @Override
    public List<TextChunkVO> search(List<String> datasetIds, List<String> excludeParagraphIds, String keyword, int maxResults, float minScore) {
        EmbeddingModel embeddingModel=datasetService.getDatasetEmbeddingModel(datasetIds.get(0));
        //todo keyword cannot be null or blank
        Response<Embedding> res = embeddingModel.embed(keyword);
        return embeddingMapper.embeddingSearch(datasetIds,excludeParagraphIds, maxResults,minScore, res.content().vector());
    }
}
