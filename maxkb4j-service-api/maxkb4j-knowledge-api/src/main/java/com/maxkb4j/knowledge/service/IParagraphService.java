package com.maxkb4j.knowledge.service;


import com.maxkb4j.knowledge.dto.ParagraphDTO;

import java.util.List;

public interface IParagraphService {
    List<ParagraphDTO> listDtoByIds(List<String> ids);
    boolean saveDtoBatch(List<ParagraphDTO> paragraphDTOList);
    boolean saveParagraphAndProblem(ParagraphDTO paragraph, List<String> problems);
    boolean deleteById(String knowledgeId,String paragraphId);
}
