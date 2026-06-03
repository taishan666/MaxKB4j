package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagService extends ServiceImpl<TagMapper, TagEntity> implements ITagService{
}
