package com.maxkb4j.knowledge.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "paragraph_keywords")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParagraphKeywords {

    @Id
    private String id;

    private String keywords;

    private String knowledgeId;

    private String documentId;

    private Boolean isActive;

    private String paragraphId;

    private Date createTime;

    private Date updateTime;

}