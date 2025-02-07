package com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.impl;

import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.NodeDetail;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.ISearchDatasetStepNode;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.dto.DatasetSetting;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.dto.SearchDatasetStepNodeParams;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseSearchDatasetNode extends ISearchDatasetStepNode {
    @Override
    public NodeResult execute(SearchDatasetStepNodeParams nodeParams, FlowParams workflowParams) {
        Map<String, Object> workflowVariable = new HashMap<>();
        // 构建节点变量
        Map<String, Object> nodeVariable = new HashMap<>();
        nodeVariable.put("paragraph_list", new ArrayList<>());
        nodeVariable.put("is_hit_handling_method_list", new ArrayList<>());
        nodeVariable.put("data", "");
        nodeVariable.put("directly_return", "");
        nodeVariable.put("question", workflowParams.getQuestion());
        return new NodeResult(nodeVariable, workflowVariable);
    }

    @Override
    public void saveContext(NodeDetail details, WorkflowManage workflowManage) {
        List<ParagraphVO> result =  details.getParagraphList();
        DatasetSetting datasetSetting = super.getNodeParamsClass(nodeParams).getDatasetSetting();

        String directlyReturn = result.stream()
                .filter(paragraph -> "directly_return".equals(paragraph.getHitHandlingMethod()))
                .map(paragraph -> String.format("%s:%s",
                        paragraph.getTitle(),
                        paragraph.getContent()))
                .collect(Collectors.joining("\n"));

        context.put("paragraph_list", result);
        context.put("question", details.getQuestion());
        context.put("run_time", details.getRuntime());

/*        List<Map<String, Object>> isHitHandlingMethodList = result.stream()
                .filter(row -> "true".equals(row.get("is_hit_handling_method")))
                .collect(Collectors.toList());
        context.put("is_hit_handling_method_list", isHitHandlingMethodList);*/

        String data = result.stream()
                .map(paragraph -> String.format("%s:%s",
                        paragraph.getTitle(),
                        paragraph.getContent()))
                .collect(Collectors.joining("\n"))
                .substring(0, Math.min(datasetSetting.getMaxParagraphCharNumber(), 5000));

        context.put("data", data);
        context.put("directly_return", directlyReturn);
    }
}
