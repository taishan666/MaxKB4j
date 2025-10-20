package com.tarzan.maxkb4j.core.workflow.node.condition.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.condition.compare.Compare;
import com.tarzan.maxkb4j.core.workflow.node.condition.compare.impl.*;

import java.util.ArrayList;
import java.util.List;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.CONDITION;

public class ConditionNode extends INode {

    static List<Compare> compareHandleList = new ArrayList<>();

    static {
        compareHandleList.add(new GECompare());
        compareHandleList.add(new GTCompare());
        compareHandleList.add(new ContainCompare());
        compareHandleList.add(new EqualCompare());
        compareHandleList.add(new LTCompare());
        compareHandleList.add(new LECompare());
        compareHandleList.add(new LengthLECompare());
        compareHandleList.add(new LengthLTCompare());
        compareHandleList.add(new LengthEqualCompare());
        compareHandleList.add(new LengthGECompare());
        compareHandleList.add(new LengthGTCompare());
        compareHandleList.add(new IsNullCompare());
        compareHandleList.add(new IsNotNullCompare());
        compareHandleList.add(new NotContainCompare());
    }

    public ConditionNode(JSONObject properties) {
        super(properties);
        this.setType(CONDITION.getKey());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("branchName", detail.get("branchName"));
    }


}
