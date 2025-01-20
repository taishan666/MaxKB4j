package com.tarzan.maxkb4j.module.embedding.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.common.dto.SearchIndex;
import com.tarzan.maxkb4j.common.dto.TSVector;
import com.tarzan.maxkb4j.common.dto.WordIndex;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.dto.ProblemDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.DatasetMapper;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.service.DocumentService;
import com.tarzan.maxkb4j.module.dataset.service.ParagraphService;
import com.tarzan.maxkb4j.module.dataset.service.ProblemParagraphService;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemParagraphVO;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.embedding.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Slf4j
@Service
public class EmbeddingService extends ServiceImpl<EmbeddingMapper, EmbeddingEntity> {

    @Autowired
    private ParagraphService paragraphService;
    @Autowired
    private ProblemParagraphService problemParagraphMappingService;
    @Autowired
    private ProblemMapper problemMapper;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private DatasetMapper datasetMapper;


    JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();

    public List<HitTestVO> dataSearch(List<UUID> datasetIds, HitTestDTO dto) {
        EmbeddingModel embeddingModel=getDatasetEmbeddingModel(datasetIds.get(0));
        Response<Embedding> res = embeddingModel.embed(dto.getQuery_text());
        if ("embedding".equals(dto.getSearch_mode())) {
            return baseMapper.embeddingSearch(datasetIds, dto, res.content().vector());
        }
        if ("keywords".equals(dto.getSearch_mode())) {
            dto.setQuery_text(toTsQuery(dto.getQuery_text()));
            return baseMapper.keywordsSearch(datasetIds, dto);
        }
        if ("blend".equals(dto.getSearch_mode())) {
            return baseMapper.HybridSearch(datasetIds, dto, res.content().vector());
        }
        return Collections.emptyList();
    }

    public List<ParagraphVO> paragraphSearch(List<UUID> datasetIds, HitTestDTO dto) {
        List<HitTestVO> list = dataSearch(datasetIds, dto);
        List<UUID> paragraphIds = list.stream().map(HitTestVO::getParagraphId).toList();
        if (CollectionUtils.isEmpty(paragraphIds)) {
            return Collections.emptyList();
        }
        Map<UUID, Double> map = list.stream().collect(Collectors.toMap(HitTestVO::getParagraphId, HitTestVO::getComprehensiveScore));
        List<ParagraphVO> paragraphs = paragraphService.retrievalParagraph(paragraphIds);
        paragraphs.forEach(e -> {
            double score = map.get(e.getId());
            e.setSimilarity(score);
            e.setComprehensiveScore(score);
        });
        return paragraphs;
    }


    public void embedParagraph(ParagraphEntity paragraph,EmbeddingModel embeddingModel) {
        if (paragraph != null) {
            List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
            log.info("开始---->向量化段落:{}", paragraph.getId());
            EmbeddingEntity paragraphEmbed = new EmbeddingEntity();
            paragraphEmbed.setId(UUID.randomUUID().toString());
            paragraphEmbed.setDatasetId(paragraph.getDatasetId());
            paragraphEmbed.setDocumentId(paragraph.getDocumentId());
            paragraphEmbed.setParagraphId((UUID) paragraph.getId());
            paragraphEmbed.setMeta(new JSONObject());
            paragraphEmbed.setSourceId(paragraph.getId().toString());
            paragraphEmbed.setSourceType("1");
            paragraphEmbed.setIsActive(paragraph.getIsActive());
            paragraphEmbed.setSearchVector(toTsVector(paragraph.getTitle() + paragraph.getContent()));
            Response<Embedding> res = embeddingModel.embed(paragraph.getTitle()+":" + paragraph.getContent());
            paragraphEmbed.setEmbedding(res.content().vectorAsList());
            embeddingEntities.add(paragraphEmbed);
            List<ProblemEntity> problems=problemParagraphMappingService.getProblemsByParagraphId(paragraph.getId());
            for (ProblemEntity problem : problems) {
                EmbeddingEntity problemEmbed = new EmbeddingEntity();
                problemEmbed.setId(UUID.randomUUID().toString());
                problemEmbed.setDatasetId(paragraph.getDatasetId());
                problemEmbed.setDocumentId(paragraph.getDocumentId());
                problemEmbed.setParagraphId(paragraph.getId());
                problemEmbed.setMeta(new JSONObject());
                problemEmbed.setSourceId(problem.getId().toString());
                problemEmbed.setSourceType("0");
                problemEmbed.setIsActive(paragraph.getIsActive());
                problemEmbed.setSearchVector(toTsVector(problem.getContent()));
                Response<Embedding> res1 = embeddingModel.embed(problem.getContent());
                problemEmbed.setEmbedding(res1.content().vectorAsList());
                embeddingEntities.add(problemEmbed);
            }
            baseMapper.insert(embeddingEntities);
            paragraphService.updateStatusById(paragraph.getId(),1,2);
            documentService.updateStatusMetaById(paragraph.getDocumentId());
            log.info("结束---->向量化段落:{}", paragraph.getId());
        }
    }

    public void embedParagraphs(List<ParagraphEntity> paragraphs,EmbeddingModel embeddingModel) {
        for (ParagraphEntity paragraph : paragraphs) {
            embedParagraph(paragraph,embeddingModel);
        }
    }

    @Transactional
    public boolean embedProblemParagraphs(List<ParagraphEntity> paragraphs, List<ProblemParagraphVO> problemParagraphs,EmbeddingModel embeddingModel) {
        List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
        for (ParagraphEntity paragraph : paragraphs) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setId(UUID.randomUUID().toString());
            embeddingEntity.setDatasetId(paragraph.getDatasetId());
            embeddingEntity.setDocumentId(paragraph.getDocumentId());
            embeddingEntity.setParagraphId(paragraph.getId());
            embeddingEntity.setMeta(new JSONObject());
            embeddingEntity.setSourceId(paragraph.getId().toString());
            embeddingEntity.setSourceType("1");
            embeddingEntity.setIsActive(paragraph.getIsActive());
            embeddingEntity.setSearchVector(toTsVector(paragraph.getTitle() + paragraph.getContent()));
            Response<Embedding> res = embeddingModel.embed(paragraph.getTitle() + paragraph.getContent());
            embeddingEntity.setEmbedding(res.content().vectorAsList());
            embeddingEntities.add(embeddingEntity);
        }
        if (!CollectionUtils.isEmpty(problemParagraphs)) {
            Map<UUID, List<ProblemParagraphVO>> map = problemParagraphs.stream().collect(Collectors.groupingBy(ProblemParagraphVO::getProblemId));
            map.forEach((k, v) -> {
                Response<Embedding> res = embeddingModel.embed(v.get(0).getContent());
                v.forEach(pp -> {
                    EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                    embeddingEntity.setId(UUID.randomUUID().toString());
                    embeddingEntity.setDatasetId(pp.getDatasetId());
                    embeddingEntity.setDocumentId(pp.getDocumentId());
                    embeddingEntity.setParagraphId(pp.getParagraphId());
                    embeddingEntity.setMeta(new JSONObject());
                    embeddingEntity.setSourceId(pp.getProblemId().toString());
                    embeddingEntity.setSourceType("0");
                    embeddingEntity.setIsActive(true);
                    embeddingEntity.setSearchVector(toTsVector(pp.getContent()));
                    embeddingEntity.setEmbedding(res.content().vectorAsList());
                    embeddingEntities.add(embeddingEntity);
                });
            });
        }
        return this.saveBatch(embeddingEntities);
    }

    @Async
    public void embedByDocIds(UUID datasetId,List<UUID> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            EmbeddingModel embeddingModel=getDatasetEmbeddingModel(datasetId);
            for (UUID docId : docIds) {
                documentService.updateStatusById(docId,1,1);
                //清除之前向量
                this.lambdaUpdate().eq(EmbeddingEntity::getDocumentId, docId).remove();
                log.info("开始--->向量化文档:{}", docId);
                List<ParagraphEntity> paragraphEntities = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
                embedParagraphs(paragraphEntities,embeddingModel);
                documentService.updateStatusById(docId,1,2);
                log.info("结束--->向量化文档:{}", docId);
            }
        }
    }


    public EmbeddingModel getDatasetEmbeddingModel(UUID datasetId){
        DatasetEntity dataset=datasetMapper.selectById(datasetId);
        return modelService.getEmbeddingModelById(dataset.getEmbeddingModeId());
    }




    private TSVector toTsVector(String text) {
        TSVector tsVector=new TSVector();
        List<String> segmentations = filterPunctuation(jiebaSegmenter.sentenceProcess(text));
        List<WordIndex> wordIndices = new ArrayList<>();
        for (int i = 0; i < segmentations.size(); i++) {
            WordIndex wordIndex = new WordIndex();
            wordIndex.setWord(segmentations.get(i));
            wordIndex.setIndex(i);
            wordIndices.add(wordIndex);
        }
        Map<String, List<WordIndex>> map = wordIndices.stream().collect(Collectors.groupingBy(WordIndex::getWord));
        Set<SearchIndex> searchVector = new HashSet<>();
        map.forEach((k, v) -> {
            SearchIndex searchIndex = new SearchIndex();
            StringBuilder sb = new StringBuilder();
            for (WordIndex w : v) {
                sb.append(w.getIndex() + 1).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            searchIndex.setWord(k);
            searchIndex.setIndices(sb.toString());
            searchVector.add(searchIndex);
        });
        tsVector.setSearchVector(searchVector);
        return tsVector;
    }


    private String toTsQuery(String text) {
        List<String> words = filterPunctuation(jiebaSegmenter.sentenceProcess(text));
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word).append("|");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }


    private static List<String> filterPunctuation(List<String> words) {
        String[] filteredWords = {"", ",", ".", "。", "=", "，", "、", "：", "；", "（", "）"};
        List<String> result = new ArrayList<>();
        for (String word : words) {
            word = word.toLowerCase().trim();
            for (String filteredWord : filteredWords) {
                if (word.contains(filteredWord)) {
                    word = word.replaceAll(filteredWord, "");
                }
            }
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        return result;
    }

    public boolean createProblem(UUID datasetId, UUID docId, UUID paragraphId, UUID problemId) {
        EmbeddingModel embeddingModel=getDatasetEmbeddingModel(datasetId);
        ProblemEntity problem = problemMapper.selectById(problemId);
        if (Objects.nonNull(problem)) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setId(UUID.randomUUID().toString());
            embeddingEntity.setDatasetId(datasetId);
            embeddingEntity.setDocumentId(docId);
            embeddingEntity.setParagraphId(paragraphId);
            embeddingEntity.setMeta(new JSONObject());
            embeddingEntity.setSourceId(problemId.toString());
            embeddingEntity.setSourceType("0");
            embeddingEntity.setIsActive(true);
            embeddingEntity.setSearchVector(toTsVector(problem.getContent()));
            Response<Embedding> res = embeddingModel.embed(problem.getContent());
            embeddingEntity.setEmbedding(res.content().vectorAsList());
            return this.save(embeddingEntity);
        }
        return false;
    }


    @Transactional
    public boolean embedByDatasetId(UUID datasetId) {
        EmbeddingModel embeddingModel=getDatasetEmbeddingModel(datasetId);
        this.lambdaUpdate().in(EmbeddingEntity::getDatasetId, datasetId).remove();
        List<ParagraphEntity> paragraphEntities = paragraphService.lambdaQuery().in(ParagraphEntity::getDatasetId, datasetId).list();
        List<ProblemParagraphVO> problemParagraphVOS = problemParagraphMappingService.getProblemsByDatasetId(datasetId);
        return embedProblemParagraphs(paragraphEntities, problemParagraphVOS,embeddingModel);
    }

    @Transactional
    public void createProblems(UUID datasetId,List<ProblemDTO> problemDTOS) {
        EmbeddingModel embeddingModel=getDatasetEmbeddingModel(datasetId);
        if (!CollectionUtils.isEmpty(problemDTOS)) {
            List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
            for (ProblemDTO problem : problemDTOS) {
                EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                embeddingEntity.setId(UUID.randomUUID().toString());
                embeddingEntity.setDatasetId(problem.getDatasetId());
                embeddingEntity.setDocumentId(problem.getDocumentId());
                embeddingEntity.setParagraphId(problem.getParagraphId());
                embeddingEntity.setMeta(new JSONObject());
                embeddingEntity.setSourceId(problem.getId().toString());
                embeddingEntity.setSourceType("0");
                embeddingEntity.setIsActive(true);
                embeddingEntity.setSearchVector(toTsVector(problem.getContent()));
                Response<Embedding> res = embeddingModel.embed(problem.getContent());
                embeddingEntity.setEmbedding(res.content().vectorAsList());
                embeddingEntities.add(embeddingEntity);
            }
            this.saveBatch(embeddingEntities);
        }
    }
}
