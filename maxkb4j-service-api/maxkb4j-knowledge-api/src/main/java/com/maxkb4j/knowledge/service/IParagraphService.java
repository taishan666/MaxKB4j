package com.maxkb4j.knowledge.service;


import com.maxkb4j.knowledge.dto.ParagraphAddDTO;
import com.maxkb4j.knowledge.dto.ParagraphDTO;

import java.util.List;

public interface IParagraphService {
    List<ParagraphDTO> listDtoByIds(List<String> ids);
    List<String> getNoActiveParagraphIds(List<String> knowledgeIds, List<String> excludeDocIds);
    boolean saveDtoBatch(List<ParagraphDTO> paragraphDTOList);
    boolean saveParagraphAndProblem(String knowledgeId, String docId, ParagraphAddDTO addDTO);
    boolean saveParagraphAndProblem(ParagraphDTO paragraph, List<String> problems);
    boolean deleteById(String knowledgeId,String paragraphId);
}
