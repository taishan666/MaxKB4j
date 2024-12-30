package com.tarzan.maxkb4j.module.embedding.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.vo.HitTestVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.embedding.mapper.EmbeddingMapper;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-30 18:08:16
 */
@Service
public class EmbeddingService extends ServiceImpl<EmbeddingMapper, EmbeddingEntity>{

    EmbeddingModel embeddingModel = new QwenEmbeddingModel(null,"sk-80611638022146a2bc9a1ec9b566cc54","text-embedding-v2");
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

    private String toTsQuery(String text){
        JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();
        List<String> words=jiebaSegmenter.sentenceProcess(text);
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word).append("|");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
