package com.maxkb4j.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.KnowledgeVersionEntity;
import com.maxkb4j.knowledge.mapper.KnowledgeVersionMapper;
import com.maxkb4j.knowledge.service.IKnowledgeVersionService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeVersionServiceImpl extends ServiceImpl<KnowledgeVersionMapper, KnowledgeVersionEntity> implements IKnowledgeVersionService {
}
