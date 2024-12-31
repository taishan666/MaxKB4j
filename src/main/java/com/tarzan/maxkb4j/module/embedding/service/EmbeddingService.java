package com.tarzan.maxkb4j.module.embedding.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.service.ParagraphService;
import com.tarzan.maxkb4j.module.dataset.service.ProblemParagraphService;
import com.tarzan.maxkb4j.module.dataset.service.ProblemService;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemParagraphVO;
import com.tarzan.maxkb4j.util.SearchIndex;
import com.tarzan.maxkb4j.util.WordIndex;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.embedding.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Service
public class EmbeddingService extends ServiceImpl<EmbeddingMapper, EmbeddingEntity>{

    @Autowired
    private ParagraphService paragraphService;
    @Autowired
    private ProblemParagraphService problemParagraphMappingService;

    EmbeddingModel embeddingModel = new QwenEmbeddingModel(null,"sk-80611638022146a2bc9a1ec9b566cc54","text-embedding-v2");
    @Autowired
    private ProblemService problemService;

    public List<HitTestVO> dataSearch(UUID id, HitTestDTO dto) {
        Response<Embedding> res= embeddingModel.embed(dto.getQuery_text());
        List<HitTestVO> list= new ArrayList<>();
        if("embedding".equals(dto.getSearch_mode())){
            list= baseMapper.embeddingSearch(id, dto,res.content().vector());
        }
        if("keywords".equals(dto.getSearch_mode())){
            dto.setQuery_text(toTsQuery(dto.getQuery_text()));
            list= baseMapper.keywordsSearch(id, dto);
        }
        if("blend".equals(dto.getSearch_mode())){
            list= baseMapper.HybridSearch(id, dto,res.content().vector());
        }
        return list;
    }


    @Transactional
    public boolean embedByDocIds(List<UUID> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            List<EmbeddingEntity> embeddingEntities=new ArrayList<>();
            this.lambdaUpdate().in(EmbeddingEntity::getDocumentId,docIds).remove();
            List<ParagraphEntity> paragraphEntities = paragraphService.lambdaQuery().in(ParagraphEntity::getDocumentId, docIds).list();
            for (ParagraphEntity paragraph: paragraphEntities) {
                EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                embeddingEntity.setId(UUID.randomUUID());
                embeddingEntity.setDatasetId(paragraph.getDatasetId());
                embeddingEntity.setDocumentId(paragraph.getDocumentId());
                embeddingEntity.setParagraphId(paragraph.getId());
                embeddingEntity.setMeta(new JSONObject());
                embeddingEntity.setSourceId(paragraph.getId().toString());
                embeddingEntity.setSourceType("1");
                embeddingEntity.setIsActive(paragraph.getIsActive());
                embeddingEntity.setSearchVector(toTsVector(paragraph.getTitle()+paragraph.getContent()));
                Response<Embedding> res= embeddingModel.embed(paragraph.getTitle()+paragraph.getContent());
                embeddingEntity.setEmbedding(res.content().vectorAsList());
                embeddingEntities.add(embeddingEntity);
            }
            List<ProblemParagraphVO> problemParagraphVOS=problemParagraphMappingService.getProblemsByDocIds(docIds);
            if (!CollectionUtils.isEmpty(problemParagraphVOS)) {
                for (ProblemParagraphVO pp : problemParagraphVOS) {
                    EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                    embeddingEntity.setId(UUID.randomUUID());
                    embeddingEntity.setDatasetId(pp.getDatasetId());
                    embeddingEntity.setDocumentId(pp.getDocumentId());
                    embeddingEntity.setParagraphId(pp.getParagraphId());
                    embeddingEntity.setMeta(new JSONObject());
                    embeddingEntity.setSourceId(pp.getProblemId().toString());
                    embeddingEntity.setSourceType("0");
                    embeddingEntity.setIsActive(true);
                    embeddingEntity.setSearchVector(toTsVector(pp.getContent()));
                    Response<Embedding> res= embeddingModel.embed(pp.getContent());
                    embeddingEntity.setEmbedding(res.content().vectorAsList());
                    embeddingEntities.add(embeddingEntity);
                }
            }
           return this.saveBatch(embeddingEntities);
        }
        return false;
    }

    JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
    private Set<SearchIndex> toTsVector(String text){
        List<String> segmentations = filterPunctuation(jiebaSegmenter.sentenceProcess(text));
        List<WordIndex> wordIndices = new ArrayList<>();
        for (int i = 0; i < segmentations.size(); i++) {
            WordIndex wordIndex = new WordIndex();
            wordIndex.setWord(segmentations.get(i));
            wordIndex.setIndex(i);
            wordIndices.add(wordIndex);
        }
        Map<String, List<WordIndex>> map = wordIndices.stream().collect(Collectors.groupingBy(WordIndex::getWord));
        Set<SearchIndex> searchVector=new HashSet<>();
        map.forEach((k,v)->{
            SearchIndex searchIndex=new SearchIndex();
            StringBuilder sb = new StringBuilder();
            for (WordIndex w : v) {
                sb.append(w.getIndex()+1).append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            searchIndex.setWord(k);
            searchIndex.setIndices(sb.toString());
            searchVector.add(searchIndex);
        });
        return searchVector;
    }



    private String toTsQuery(String text){
        List<String> words=filterPunctuation(jiebaSegmenter.sentenceProcess(text));
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word).append("|");
        }
        sb.deleteCharAt(sb.length()-1);
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
        ProblemEntity problem=problemService.getById(problemId);
        if (Objects.nonNull(problem)) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setDatasetId(datasetId);
            embeddingEntity.setDocumentId(docId);
            embeddingEntity.setParagraphId(paragraphId);
            embeddingEntity.setMeta(new JSONObject());
            embeddingEntity.setSourceId(problemId.toString());
            embeddingEntity.setSourceType("0");
            embeddingEntity.setIsActive(true);
            embeddingEntity.setSearchVector(toTsVector(problem.getContent()));
            Response<Embedding> res= embeddingModel.embed(problem.getContent());
            embeddingEntity.setEmbedding(res.content().vectorAsList());
            this.save(embeddingEntity);
        }
    }
}
