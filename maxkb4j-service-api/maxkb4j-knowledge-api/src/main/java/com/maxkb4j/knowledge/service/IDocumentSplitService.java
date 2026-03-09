package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.dto.ParagraphSimple;

import java.util.List;

public interface IDocumentSplitService {

    List<ParagraphSimple> split(String docText, String[] patterns, Integer limit, Boolean withFilter);
}
