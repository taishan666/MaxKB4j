package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.enums.AppType;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.application.util.WorkFlowNodes;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.application.vo.KnowledgeVO;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.knowledge.dto.KnowledgeSimple;
import com.maxkb4j.knowledge.service.IKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.maxkb4j.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

/**
 * 应用详情 VO 组装（填充关联知识库列表），从 {@link ApplicationServiceImpl} 抽离。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ApplicationDetailAssembler {

    private final IKnowledgeService knowledgeService;

    /**
     * 为应用详情填充关联知识库信息。
     */
    public ApplicationVO wrap(ApplicationVO vo) {
        if (vo == null) {
            return null;
        }
        if (AppType.WORK_FLOW.name().equals(vo.getType())) {
            fillWorkFlowKnowledgeList(vo.getWorkFlow());
        } else {
            fillKnowledgeList(vo);
        }
        return vo;
    }

    private void fillWorkFlowKnowledgeList(JSONObject workFlow) {
        JSONArray nodes = WorkFlowNodes.getNodes(workFlow);
        if (nodes == null) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (node == null || !SEARCH_KNOWLEDGE.getKey().equals(node.getString("type"))) {
                continue;
            }
            JSONObject nodeData = WorkFlowNodes.getNodeData(node);
            if (nodeData == null) {
                continue;
            }
            nodeData.put("knowledgeList", List.of());
            JSONArray knowledgeIdArray = nodeData.getJSONArray("knowledgeIds");
            if (knowledgeIdArray == null) {
                continue;
            }
            List<String> knowledgeIds = knowledgeIdArray.toJavaList(String.class);
            if (!CollectionUtils.isEmpty(knowledgeIds)) {
                nodeData.put("knowledgeList", knowledgeService.listSimpleKnowledgeByIds(knowledgeIds));
            }
        }
    }

    private void fillKnowledgeList(ApplicationVO vo) {
        List<String> knowledgeIds = vo.getKnowledgeIds();
        if (CollectionUtils.isEmpty(knowledgeIds)) {
            vo.setKnowledgeList(List.of());
            return;
        }
        List<KnowledgeSimple> knowledgeList = knowledgeService.listSimpleKnowledgeByIds(knowledgeIds);
        vo.setKnowledgeList(BeanUtil.copyList(knowledgeList, KnowledgeVO.class));
    }
}
