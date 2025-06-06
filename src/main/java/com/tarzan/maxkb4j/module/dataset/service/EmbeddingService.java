package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.core.common.dto.SearchIndex;
import com.tarzan.maxkb4j.core.common.dto.TSVector;
import com.tarzan.maxkb4j.core.common.dto.WordIndex;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.dataset.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemParagraphVO;
import com.tarzan.maxkb4j.module.dataset.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.dataset.mapper.EmbeddingMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
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
@AllArgsConstructor
public class EmbeddingService extends ServiceImpl<EmbeddingMapper, EmbeddingEntity> {

    private final ParagraphMapper paragraphMapper;
    private final ProblemParagraphMapper problemParagraphMappingMapper;
    private final ProblemMapper problemMapper;
    private final JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();


    @Transactional
    public boolean embedProblemParagraphs(List<ParagraphEntity> paragraphs, List<ProblemParagraphVO> problemParagraphs,EmbeddingModel embeddingModel) {
        List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
        for (ParagraphEntity paragraph : paragraphs) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setDatasetId(paragraph.getDatasetId());
            embeddingEntity.setDocumentId(paragraph.getDocumentId());
            embeddingEntity.setParagraphId(paragraph.getId());
            embeddingEntity.setMeta(new JSONObject());
            embeddingEntity.setSourceId(paragraph.getId());
            embeddingEntity.setSourceType("1");
            embeddingEntity.setIsActive(paragraph.getIsActive());
          //  embeddingEntity.setSearchVector(toTsVector(paragraph.getTitle() + paragraph.getContent()));
            embeddingModel.embed(paragraph.getTitle() + paragraph.getContent());
            Response<Embedding> res;
            if(StringUtil.isNotBlank(paragraph.getTitle())){
                res = embeddingModel.embed(paragraph.getTitle() + paragraph.getContent());
            }else {
                res = embeddingModel.embed(paragraph.getContent());
            }
            embeddingEntity.setEmbedding(res.content().vectorAsList());
            embeddingEntities.add(embeddingEntity);
        }
        if (!CollectionUtils.isEmpty(problemParagraphs)) {
            Map<String, List<ProblemParagraphVO>> map = problemParagraphs.stream().collect(Collectors.groupingBy(ProblemParagraphVO::getProblemId));
            map.forEach((k, v) -> {
                Response<Embedding> res = embeddingModel.embed(v.get(0).getContent());
                v.forEach(pp -> {
                    EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                    embeddingEntity.setDatasetId(pp.getDatasetId());
                    embeddingEntity.setDocumentId(pp.getDocumentId());
                    embeddingEntity.setParagraphId(pp.getParagraphId());
                    embeddingEntity.setMeta(new JSONObject());
                    embeddingEntity.setSourceId(pp.getProblemId());
                    embeddingEntity.setSourceType("0");
                    embeddingEntity.setIsActive(true);
                  //  embeddingEntity.setSearchVector(toTsVector(pp.getContent()));
                    embeddingEntity.setEmbedding(res.content().vectorAsList());
                    embeddingEntities.add(embeddingEntity);
                });
            });
        }
        return this.saveBatch(embeddingEntities);
    }


    public TSVector toTsVector(String text) {
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


    public boolean createProblem(String datasetId, String docId, String paragraphId, String problemId,EmbeddingModel embeddingModel) {
        ProblemEntity problem = problemMapper.selectById(problemId);
        if (Objects.nonNull(problem)) {
            EmbeddingEntity embeddingEntity = new EmbeddingEntity();
            embeddingEntity.setDatasetId(datasetId);
            embeddingEntity.setDocumentId(docId);
            embeddingEntity.setParagraphId(paragraphId);
            embeddingEntity.setMeta(new JSONObject());
            embeddingEntity.setSourceId(problemId);
            embeddingEntity.setSourceType("0");
            embeddingEntity.setIsActive(true);
         //   embeddingEntity.setSearchVector(toTsVector(problem.getContent()));
            Response<Embedding> res = embeddingModel.embed(problem.getContent());
            embeddingEntity.setEmbedding(res.content().vectorAsList());
            return this.save(embeddingEntity);
        }
        return false;
    }


    @Transactional
    public boolean embedByDatasetId(String datasetId,EmbeddingModel embeddingModel) {
        this.lambdaUpdate().in(EmbeddingEntity::getDatasetId, datasetId).remove();
        List<ParagraphEntity> paragraphEntities = paragraphMapper.selectList(Wrappers.<ParagraphEntity>lambdaQuery().in(ParagraphEntity::getDatasetId, datasetId));
        List<ProblemParagraphVO> problemParagraphVOS = problemParagraphMappingMapper.getProblems(datasetId,null);
        return embedProblemParagraphs(paragraphEntities, problemParagraphVOS,embeddingModel);
    }

   /* @Transactional
    public void createProblems(EmbeddingModel embeddingModel,List<ProblemEntity> problems,String docId,String paragraphId) {
        if (!CollectionUtils.isEmpty(problems)) {
            List<EmbeddingEntity> embeddingEntities = new ArrayList<>();
            for (ProblemEntity problem : problems) {
                EmbeddingEntity embeddingEntity = new EmbeddingEntity();
                embeddingEntity.setDatasetId(problem.getDatasetId());
                embeddingEntity.setDocumentId(docId);
                embeddingEntity.setParagraphId(paragraphId);
                embeddingEntity.setMeta(new JSONObject());
                embeddingEntity.setSourceId(problem.getId());
                embeddingEntity.setSourceType("0");
                embeddingEntity.setIsActive(true);
              //  embeddingEntity.setSearchVector(toTsVector(problem.getContent()));
                Response<Embedding> res = embeddingModel.embed(problem.getContent());
                embeddingEntity.setEmbedding(res.content().vectorAsList());
                embeddingEntities.add(embeddingEntity);
            }
            this.saveBatch(embeddingEntities);
        }
    }
*/
    private static List<String> filterPunctuation(List<String> words) {
        var filteredWords = new String[]{"", ",", ".", "。", "=", "，", "、", "：", "；", "（", "）"};
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

    public List<HitTestVO> embeddingSearch(List<String> datasetIds,int maxResults, double minScore,float[]  referenceEmbedding){
        return baseMapper.embeddingSearch(datasetIds,maxResults,minScore,referenceEmbedding);
    }
}
