package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.dto.DocumentSimple;

import java.util.List;

public interface IDocumentWebService {
    List<DocumentSimple> getDocumentList(String sourceUrl, String selector, boolean isRecursive);
}
