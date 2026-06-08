package com.maxkb4j.knowledge.vo;

import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DocumentVO extends DocumentEntity {
    private Integer paragraphCount;
    private Integer tagCount;
    private List<TagEntity> tags;
}
