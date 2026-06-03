package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.mapper.DocumentTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DocumentTagService extends ServiceImpl<DocumentTagMapper,DocumentTagEntity> implements IDocumentTagService{
}
