package com.tarzan.maxkb4j.core.workflow.node.reranker.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.reranker.input.RerankerParams;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseReranker;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RerankerNode extends INode {

    private final ModelService modelService;

    public RerankerNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public String getType() {
        return "reranker-node";
    }

    @Override
    public NodeResult execute() {
        RerankerParams nodeParams= super.nodeParams.toJavaObject(RerankerParams.class);
        List<String> questionReferenceAddress=nodeParams.getQuestionReferenceAddress();
        Object question = super.getWorkflowManage().getReferenceField(questionReferenceAddress.get(0), questionReferenceAddress.subList(1, questionReferenceAddress.size()));
        List<List<String>> rerankerReferenceList=nodeParams.getRerankerReferenceList();
        List<Object>  rerankerList = getRerankerList(rerankerReferenceList);
       // 合并重排序器列表
        List<String> documents = mergeRerankerList(rerankerList);
     // 获取 top_n 参数，默认值为 3
        int topN =  nodeParams.getRerankerSetting().getTopN();

        // 构建上下文
/*        List<Map<String, String>> documentList = documents.stream()
                .map(doc -> Map.of(
                        "page_content", doc.getContent(),
                        "metadata", doc.getTitle()))
                .toList();*/

        // 获取重排序模型实例
        BaseReranker rerankerModel = modelService.getModelById(nodeParams.getRerankerModelId());

        // 压缩文档
        List<Map<String,Object>> resultList = rerankerModel.textReRank(question.toString(),documents);

        // 获取相似度阈值，默认值为 0.6
     /*   double similarity = nodeParams.getRerankerSetting().getSimilarity();

        // 获取最大段落字符数，默认值为 5000
        int maxParagraphCharNumber = nodeParams.getRerankerSetting().getMaxParagraphCharNumber();

        // 重置结果列表
        result = resetResultList(result, documents);

        // 过滤结果
        List<Map<String, Object>> filteredResult = filterResult(result, maxParagraphCharNumber, topN, similarity);

        // 构建返回结果
        String concatenatedResult = filteredResult.stream()
                .map(item -> (String) item.get("page_content"))
                .collect(Collectors.joining());*/
        StringBuilder sb=new StringBuilder();
        for (Map<String, Object> map : resultList) {
            sb.append(map.get("text"));
        }
        return new NodeResult(Map.of("result_list", resultList,
                "result",sb.toString(),
                "document_list", documents,
                "question",question), Map.of());
    }

    public List<Object> getRerankerList(List<List<String>> rerankerReferenceList) {
        // 使用流式操作实现类似 Python 的列表推导式
        return rerankerReferenceList.stream()
                .map(reference -> {
                    // 调用 workflowManage 的方法获取 reference_field
                    // 第一个元素
                    // 剩余部分
                    return workflowManage.getReferenceField(
                            reference.get(0), // 第一个元素
                            reference.subList(1, reference.size()) // 剩余部分
                    );
                })
                .collect(Collectors.toList());
    }

    private List<String> mergeRerankerList(List<Object>  rerankerList) {
        List<String> result=new ArrayList<>();
        for (Object object : rerankerList) {
            System.out.println(object);
            if (object instanceof List<?> list){
                for (Object o : list) {
                    ParagraphVO paragraph= (ParagraphVO)o;
                    result.add(paragraph.getTitle()+"-"+paragraph.getContent());
                }
            }
        }
        // 实现重置结果列表的逻辑
        return result;
    }

/*
    private List<Map<String, Object>> resetResultList(List<Map<String, Object>> result, List<Document> documents) {
        // 实现重置结果列表的逻辑
        return result;
    }
*/

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("document_list", context.get("document_list"));
        detail.put("question", context.get("question"));
        detail.put("reranker_setting", context.get("content"));
        detail.put("result", context.get("answer"));
        detail.put("result_list", context.get("audio_list"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
