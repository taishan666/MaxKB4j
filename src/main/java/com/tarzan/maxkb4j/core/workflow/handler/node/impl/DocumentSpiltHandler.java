package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.DocumentSpiltNode;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphSimple;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentSpiltService;
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
public class DocumentSpiltHandler implements INodeHandler {

    private final DocumentSpiltService documentSpiltService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        DocumentSpiltNode.NodeParams nodeParams = node.getNodeData().toJavaObject(DocumentSpiltNode.NodeParams.class);
        List<String> fileIds = nodeParams.getDocumentList();
        Object res = workflow.getReferenceField(fileIds);
        List<DocumentSimple> documentList = res == null ? List.of() : (List<DocumentSimple>) res;
        for (DocumentSimple document : documentList) {
            if ("qa".equals(nodeParams.getSplitStrategy())) {
                qaSplit(document, nodeParams.getChunkSize());
            } else {
                defaultSplit(document, nodeParams.getPatterns(), nodeParams.getChunkSize(), nodeParams.getWithFilter());
            }
        }
        boolean paragraphTitleRelateProblem = Boolean.TRUE.equals(nodeParams.getParagraphTitleRelateProblem());
        boolean documentNameRelateProblem = Boolean.TRUE.equals(nodeParams.getDocumentNameRelateProblem());
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
        node.getDetail().put("splitStrategy", nodeParams.getSplitStrategy());
        node.getDetail().put("chunkSize", nodeParams.getChunkSize());
        node.getDetail().put("documentList", documentList);
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
                continue; // 无论是表头还是分隔行，都跳过
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
            ParagraphSimple paragraph = ParagraphSimple.builder().title(title).content(contentText).problemList(questions).build();
            paragraphs.add(paragraph);
        }
        document.setParagraphs(paragraphs);
    }

}
