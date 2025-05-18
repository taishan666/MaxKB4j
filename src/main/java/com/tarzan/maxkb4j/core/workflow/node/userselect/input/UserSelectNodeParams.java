package com.tarzan.maxkb4j.core.workflow.node.userselect.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class UserSelectNodeParams {
    private List<UserSelectBranch> branch;
    private JSONObject formData;
    private String labelName;
    private Boolean isResult;
}

