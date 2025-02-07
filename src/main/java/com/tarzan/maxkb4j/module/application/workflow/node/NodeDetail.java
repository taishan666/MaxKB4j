package com.tarzan.maxkb4j.module.application.workflow.node;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NodeDetail {

    private Integer index;

    private String nodeId;

    private String runtimeNodeId;

    private String question;
    private String answer;

    private long runtime;
    private String branchId;
    private String branchName;
    private List<String> lastNodeIdList;

    private JSONObject applicationNodeDict;

    private List<ParagraphVO> paragraphList;


    private List<Object> imageList = new ArrayList<>();
    private List<Object> documentList = new ArrayList<>();
    private List<Object> audioList = new ArrayList<>();
}
