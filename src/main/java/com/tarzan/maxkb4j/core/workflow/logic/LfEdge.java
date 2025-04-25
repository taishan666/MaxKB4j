package com.tarzan.maxkb4j.core.workflow.logic;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class LfEdge {
    private String id;
    private String type;
    private String sourceNodeId;
    private String targetNodeId;
    private LfPoint startPoint;
    private LfPoint endPoint;
    private List<LfPoint> pointsList;
    private JSONObject properties;
    private String sourceAnchorId;
    private String targetAnchorId;

}

