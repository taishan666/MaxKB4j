package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.excel.KnowledgeExcel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KnowledgeExcelDataBuilder {

    private final ParagraphService paragraphService;
    private final ProblemParagraphService problemParagraphService;

    public List<KnowledgeExcel> buildByDocId(String docId) {
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery()
                .eq(ParagraphEntity::getDocumentId, docId)
                .list();
        return paragraphs.stream().map(this::toExcel).collect(Collectors.toList());
    }

    private KnowledgeExcel toExcel(ParagraphEntity paragraph) {
        KnowledgeExcel excel = new KnowledgeExcel();
        excel.setTitle(paragraph.getTitle());
        excel.setContent(paragraph.getContent());
        List<ProblemEntity> problems = problemParagraphService.getProblemsByParagraphId(paragraph.getId());
        if (!CollectionUtils.isEmpty(problems)) {
            excel.setProblems(problems.stream()
                    .map(ProblemEntity::getContent)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.joining("\n")));
        }
        return excel;
    }
}
