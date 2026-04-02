package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import com.maxkb4j.knowledge.service.IDocumentSplitService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.DocumentSpiltNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@NodeHandlerType(NodeType.DOCUMENT_SPLIT)
@RequiredArgsConstructor
public class DocumentSpiltHandler extends AbsNodeHandler {

    private final IDocumentSplitService documentSpiltService;

    @SuppressWarnings("unchecked")
    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        DocumentSpiltNode.NodeParams params = parseParams(node, DocumentSpiltNode.NodeParams.class);
        List<String> fileIds = params.getDocumentList();
        Object res = workflow.getReferenceField(fileIds);
        List<DocumentSimple> documentList = res == null ? new ArrayList<>() : (List<DocumentSimple>) res;

        for (DocumentSimple document : documentList) {
            if ("qa".equals(params.getSplitStrategy())) {
                qaSplit(document, params.getChunkSize());
            } else {
                defaultSplit(document, params.getPatterns(), params.getChunkSize(), params.getWithFilter());
            }
        }

        boolean paragraphTitleRelateProblem = Boolean.TRUE.equals(params.getParagraphTitleRelateProblem());
        boolean documentNameRelateProblem = Boolean.TRUE.equals(params.getDocumentNameRelateProblem());

        if (paragraphTitleRelateProblem || documentNameRelateProblem) {
            documentList.forEach(document -> document.getParagraphs().forEach(paragraph -> {
                List<String> problemList = paragraph.getProblemList();
                if (problemList == null) {
                    problemList = new ArrayList<>();
                    paragraph.setProblemList(problemList);
                }
                if (paragraphTitleRelateProblem && StringUtils.isNotBlank(paragraph.getTitle())) {
                    problemList.add(paragraph.getTitle());
                }
                if (documentNameRelateProblem && StringUtils.isNotBlank(document.getName())) {
                    problemList.add(document.getName());
                }
            }));
        }

        putDetails(node, Map.of(
                "splitStrategy", params.getSplitStrategy(),
                "chunkSize", params.getChunkSize(),
                "documentList", documentList
        ));

        return new NodeResult(Map.of("paragraphList", documentList));
    }

    private void defaultSplit(DocumentSimple document, String[] patterns, int chunkSize, boolean withFilter) {
        String content = document.getContent();
        List<ParagraphSimple> paragraphs = documentSpiltService.split(content, patterns, chunkSize, withFilter);
        document.setParagraphs(paragraphs);
    }

    private void qaSplit(DocumentSimple document, int chunkSize) {
        String resContent = document.getContent() != null ? document.getContent() : "";
        String[] lines = resContent.split("\n");
        boolean dataStarted = false;
        List<ParagraphSimple> paragraphs = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("|") || !line.endsWith("|")) {
                continue;
            }
            // 跳过表头：识别分隔行（如 | --- | --- |）来标志数据开始
            if (!dataStarted) {
                if (line.contains("---")) {
                    dataStarted = true;
                }
                continue;
            }
            // 处理数据行
            String content = line.substring(1, line.length() - 1).trim();
            String[] cells = Arrays.stream(content.split("\\|"))
                    .map(String::trim)
                    .toArray(String[]::new);

            if (cells.length < 3) continue;

            String title = cells[0].isEmpty() ? null : cells[0];
            String contentText = cells[1];
            String questionsCell = cells[2];

            List<String> questions = questionsCell.isEmpty() ? Collections.emptyList() :
                    Arrays.stream(questionsCell.split("<br>"))
                            .map(String::trim)
                            .filter(q -> !q.isEmpty())
                            .collect(Collectors.toList());

            ParagraphSimple paragraph = ParagraphSimple.builder()
                    .title(title)
                    .content(contentText)
                    .problemList(questions)
                    .build();
            paragraphs.add(paragraph);
        }
        document.setParagraphs(paragraphs);
    }
}
