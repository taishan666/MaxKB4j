package com.tarzan.maxkb4j.module.dataset.service;

import com.tarzan.maxkb4j.module.dataset.dto.DatasetBatchHitHandlingDTO;
import com.tarzan.maxkb4j.module.dataset.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class EmbedTextService {


    private final DocumentService documentService;
    private final DatasetBaseService datasetService;
    private final ParagraphService paragraphService;
    private final ProblemService problemService;
    private final ModelService modelService;
    private final EmbeddingService embeddingService;

    public boolean refresh(String datasetId, String docId) {
        documentService.embedByDocIds(datasetService.getDatasetEmbeddingModel(datasetId), List.of(docId));
        return true;
    }

    public boolean batchRefresh(String datasetId, DatasetBatchHitHandlingDTO dto) {
        documentService.embedByDocIds(datasetService.getDatasetEmbeddingModel(datasetId), dto.getIdList());
        return true;
    }

    public boolean reEmbedding(String datasetId) {
        return embeddingService.embedByDatasetId(datasetId, datasetService.getDatasetEmbeddingModel(datasetId));
    }


    public boolean batchGenerateRelated(String datasetId, GenerateProblemDTO dto) {
        if (CollectionUtils.isEmpty(dto.getDocumentIdList())) {
            return false;
        }
        for (String docId : dto.getDocumentIdList()) {
            paragraphService.updateStatusByDocId(docId, 2, 0);
            documentService.updateStatusById(docId, 2, 0);
            documentService.updateStatusMetaById(docId);
        }
    /*    paragraphService.updateStatusByDocIds(dto.getDocumentIdList(), 2, 0);
        documentService.updateStatusMetaByIds(dto.getDocumentIdList());
        documentService.updateStatusByIds(dto.getDocumentIdList(), 2, 0);*/
        DatasetEntity dataset = datasetService.getById(datasetId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModelId());
        EmbeddingModel embeddingModel = modelService.getModelById(dataset.getEmbeddingModelId());
        dto.getDocumentIdList().parallelStream().forEach(docId -> {
            List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
            List<ProblemEntity> docProblems = new ArrayList<>();
            List<ProblemEntity> dbProblemEntities = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
            documentService.updateStatusById(docId, 2, 1);
            paragraphs.parallelStream().forEach(paragraph -> {
                problemService.generateRelated(chatModel, embeddingModel, datasetId, docId, paragraph, dbProblemEntities, docProblems, dto);
                paragraphService.updateStatusById(paragraph.getId(), 2, 2);
                documentService.updateStatusMetaById(paragraph.getDocumentId());
            });
            documentService.updateStatusById(docId, 2, 2);
        });
        return true;
    }


    public boolean paragraphBatchGenerateRelated(String datasetId, String docId, GenerateProblemDTO dto) {
       // paragraphService.updateStatusByDocIds(List.of(docId), 2, 0);
      //  documentService.updateStatusMetaByIds(List.of(docId));
      //  documentService.updateStatusByIds(List.of(docId), 2, 0);
        DatasetEntity dataset = datasetService.getById(datasetId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModelId());
        EmbeddingModel embeddingModel = modelService.getModelById(dataset.getEmbeddingModelId());
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
        List<ProblemEntity> docProblems = new ArrayList<>();
        List<ProblemEntity> dbProblemEntities = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
        documentService.updateStatusById(docId, 2, 1);
        paragraphs.parallelStream().forEach(paragraph -> {
            problemService.generateRelated(chatModel, embeddingModel, datasetId, docId, paragraph, dbProblemEntities, docProblems, dto);
            documentService.updateStatusMetaById(paragraph.getDocumentId());
        });
        documentService.updateStatusById(docId, 2, 2);
        return true;
    }


    public List<HitTestVO> search(List<String> datasetIds, String keyword,int maxResults,float minScore) {
        EmbeddingModel embeddingModel=datasetService.getDatasetEmbeddingModel(datasetIds.get(0));
        Response<Embedding> res = embeddingModel.embed(keyword);
        return embeddingService.embeddingSearch(datasetIds, maxResults,minScore, res.content().vector());
    }

    public List<HitTestVO> search(EmbeddingModel embeddingModel,List<String> datasetIds, String keyword,int maxResults,float minScore) {
        Response<Embedding> res = embeddingModel.embed(keyword);
        return embeddingService.embeddingSearch(datasetIds, maxResults,minScore, res.content().vector());
    }



}
