package com.tarzan.maxkb4j.module.dataset.service.impl;

import com.tarzan.maxkb4j.module.dataset.domain.vo.TextChunkVO;
import com.tarzan.maxkb4j.module.dataset.consts.SearchType;
import com.tarzan.maxkb4j.module.dataset.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.dataset.service.DatasetBaseService;
import com.tarzan.maxkb4j.module.dataset.service.IDataRetriever;
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
        Response<Embedding> res = embeddingModel.embed(keyword);
        return embeddingMapper.embeddingSearch(datasetIds,excludeParagraphIds, maxResults,minScore, res.content().vector());
    }
}
