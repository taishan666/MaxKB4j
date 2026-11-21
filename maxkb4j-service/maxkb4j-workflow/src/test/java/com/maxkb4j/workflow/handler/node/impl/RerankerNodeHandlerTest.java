package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.impl.RerankerNode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.MetadataField;
import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Reranker 节点多路召回引用解析的回归测试：
 * <ul>
 *   <li>引用值为 null（上游被 SKIP/配置错误）时该路安全跳过，不抛 NPE；</li>
 *   <li>引用值来自恢复执行（JSONArray/JSONObject）时经 fastjson 重建为 POJO；</li>
 *   <li>全部引用无可用段落或问题为空时，跳过远程打分模型调用并返回空结果，避免报与真实原因无关的异常。</li>
 * </ul>
 */
class RerankerNodeHandlerTest {

    private static final List<String> QUESTION_REF = List.of("global", "question");
    private static final List<String> PARAGRAPH_REF = List.of("node-a", "paragraphList");

    private final ScoringModel scoringModel = mock(ScoringModel.class);
    private final IModelProviderService modelFactory = mock(IModelProviderService.class);
    private final IWorkflow workflow = mock(IWorkflow.class);

    private final RerankerNodeHandler handler = new RerankerNodeHandler(modelFactory);

    private RerankerNode node() {
        JSONObject nodeData = JSON.parseObject("""
                {
                  "rerankerSetting": {"topN": 2, "similarity": 0.5, "maxParagraphCharNumber": 100},
                  "questionReferenceAddress": ["global", "question"],
                  "rerankerModelId": "m1",
                  "rerankerReferenceList": [["node-a", "paragraphList"]]
                }
                """);
        return new RerankerNode("reranker-1", new JSONObject(Map.of("nodeData", nodeData)));
    }

    /**
     * 引用取值桩：引用地址末段为 question 返回问题值，否则返回段落值
     */
    private void stubReference(Object questionValue, Object paragraphValue) {
        when(workflow.getReferenceField(any())).thenAnswer(inv -> {
            List<?> ref = inv.getArgument(0);
            if (ref != null && !ref.isEmpty() && "question".equals(ref.get(ref.size() - 1))) {
                return questionValue;
            }
            return paragraphValue;
        });
    }

    @Test
    void getRerankerList_skipsPathWhenReferenceMissing() {
        stubReference("问题", null);

        List<TextSegment> segments = handler.getRerankerList(workflow, List.of(PARAGRAPH_REF));

        assertThat(segments).isEmpty();
    }

    @Test
    void getRerankerList_rebuildsParagraphsFromRecoveredJson() {
        JSONArray recovered = JSON.parseArray("""
                [
                  {"content": "段落内容一", "title": "标题一", "similarity": 0.8,
                   "knowledgeType": 0, "knowledgeName": "知识库A", "documentName": "文档A", "isActive": true},
                  {"content": "   "}
                ]
                """);
        stubReference("问题", recovered);

        List<TextSegment> segments = handler.getRerankerList(workflow, List.of(PARAGRAPH_REF));

        // 空白内容段落被过滤
        assertThat(segments).hasSize(1);
        TextSegment segment = segments.get(0);
        assertThat(segment.text()).isEqualTo("段落内容一");
        Map<String, Object> metadata = segment.metadata().toMap();
        assertThat(metadata)
                .containsEntry(MetadataField.TITLE, "标题一")
                .containsEntry(MetadataField.SIMILARITY, 0.8)
                .containsEntry(MetadataField.KNOWLEDGE_TYPE, 0)
                .containsEntry(MetadataField.KNOWLEDGE_NAME, "知识库A")
                .containsEntry(MetadataField.DOCUMENT_NAME, "文档A")
                .containsEntry(MetadataField.IS_ACTIVE, "true");
    }

    @Test
    void getRerankerList_passesThroughLivePojoList() {
        ParagraphRagVO paragraph = new ParagraphRagVO();
        paragraph.setContent("运行期段落");
        stubReference("问题", List.of(paragraph));

        List<TextSegment> segments = handler.getRerankerList(workflow, List.of(PARAGRAPH_REF));

        assertThat(segments).hasSize(1);
        assertThat(segments.get(0).text()).isEqualTo("运行期段落");
    }

    @Test
    void doExecute_skipsScoringWhenAllParagraphReferencesMissing() throws Exception {
        stubReference("问题", null);

        NodeResult result = handler.doExecute(workflow, node());

        assertThat(result.getNodeVariable())
                .containsEntry(NodeField.RESULT, "")
                .containsEntry(NodeField.RESULT_LIST, List.of());
        // 未构建打分模型、未发起远程调用
        verifyNoInteractions(modelFactory);
    }

    @Test
    void doExecute_skipsScoringWhenQuestionMissing() throws Exception {
        ParagraphRagVO paragraph = new ParagraphRagVO();
        paragraph.setContent("段落内容");
        stubReference(null, List.of(paragraph));

        NodeResult result = handler.doExecute(workflow, node());

        assertThat(result.getNodeVariable())
                .containsEntry(NodeField.RESULT, "")
                .containsEntry(NodeField.RESULT_LIST, List.of());
        verifyNoInteractions(modelFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    void doExecute_scoresFiltersSortsAndAppliesTopN() throws Exception {
        RerankerNode rerankerNode = node();
        // 段落 similarity 为 null：恢复场景下 JSON 元素缺字段，应兜底 0 而非让 Metadata 构建抛错
        ParagraphRagVO low = new ParagraphRagVO();
        low.setContent("内容A");
        ParagraphRagVO high = new ParagraphRagVO();
        high.setContent("内容B");
        stubReference("问题", List.of(low, high));
        when(modelFactory.buildScoringModel("m1")).thenReturn(scoringModel);
        when(scoringModel.scoreAll(anyList(), eq("问题"))).thenReturn(Response.from(List.of(0.2, 0.9)));

        NodeResult result = handler.doExecute(workflow, rerankerNode);

        // 0.2 < similarity 0.5 被过滤，0.9 保留
        List<RerankerNode.RerankResult> resultList =
                (List<RerankerNode.RerankResult>) result.getNodeVariable().get(NodeField.RESULT_LIST);
        assertThat(resultList).hasSize(1);
        assertThat(resultList.get(0).getPageContent()).isEqualTo("内容B");
        assertThat(resultList.get(0).getMetadata()).containsEntry(NodeField.RELEVANCE_SCORE, 0.9);
        assertThat(result.getNodeVariable()).containsEntry(NodeField.RESULT, "内容B");

        assertThat(rerankerNode.getDetail())
                .containsEntry(NodeField.QUESTION, "问题");
        List<RerankerNode.RerankResult> documentList =
                (List<RerankerNode.RerankResult>) rerankerNode.getDetail().get(NodeField.DOCUMENT_LIST);
        assertThat(documentList).hasSize(2);
    }

    @Test
    void doExecute_neverCallsScoringModelWithEmptySegments() throws Exception {
        stubReference("问题", new JSONArray());

        handler.doExecute(workflow, node());

        verify(scoringModel, never()).scoreAll(anyList(), any());
    }
}
