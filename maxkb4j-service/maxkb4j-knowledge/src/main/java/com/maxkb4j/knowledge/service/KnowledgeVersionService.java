package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.KnowledgeVersionEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeVersionMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeVersionService extends ServiceImpl<KnowledgeVersionMapper, KnowledgeVersionEntity> {
}
