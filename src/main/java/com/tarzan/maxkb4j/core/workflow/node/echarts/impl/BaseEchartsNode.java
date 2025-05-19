package com.tarzan.maxkb4j.core.workflow.node.echarts.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.echarts.input.EchartsNodeParams;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.ECHARTS;

public class BaseEchartsNode extends INode {


    @Override
    public NodeResult execute() {
        System.out.println(ECHARTS);
        EchartsNodeParams nodeParams = super.nodeParams.toJavaObject(EchartsNodeParams.class);
        //饼状图
        //   String formSetting="{\"actionType\":\"JSON\",\"style\":{\"height\":\"400px\"},\"option\":{\"title\":{\"text\":\"访问来源分布\",\"left\":\"center\"},\"tooltip\":{\"trigger\":\"item\"},\"legend\":{\"top\":\"5%\",\"left\":\"center\",\"data\":[\"直接访问\",\"联盟广告\",\"搜索引擎\"]},\"series\":[{\"name\":\"访问来源\",\"type\":\"pie\",\"radius\":\"50%\",\"data\":[{\"value\":335,\"name\":\"直接访问\"},{\"value\":234,\"name\":\"联盟广告\"},{\"value\":1548,\"name\":\"搜索引擎\"}],\"emphasis\":{\"itemStyle\":{\"shadowBlur\":10,\"shadowOffsetX\":0,\"shadowColor\":\"rgba(0, 0, 0, 0.5)\"}}}]}}";
        //柱状图
        //    String formSetting="{\"actionType\":\"JSON\",\"style\":{\"height\":\"400px\",\"width\":\"100%\"},\"option\":{\"title\":{\"text\":\"年度销售数据\",\"subtext\":\"虚拟数据\",\"left\":\"center\"},\"tooltip\":{\"trigger\":\"axis\",\"axisPointer\":{\"type\":\"shadow\"}},\"legend\":{\"data\":[\"产品A\",\"产品B\"],\"top\":\"5%\",\"left\":\"center\"},\"xAxis\":[{\"type\":\"category\",\"data\":[\"第一季度\",\"第二季度\",\"第三季度\",\"第四季度\"],\"axisTick\":{\"alignWithLabel\":true}}],\"yAxis\":[{\"type\":\"value\"}],\"series\":[{\"name\":\"产品A\",\"type\":\"bar\",\"data\":[520,632,501,734]},{\"name\":\"产品B\",\"type\":\"bar\",\"data\":[450,580,490,700]}],\"color\":[\"#5470C6\",\"#91CC75\"]}}";
        //折线图
        String formSetting="{\"actionType\":\"JSON\",\"style\":{\"height\":\"400px\",\"width\":\"100%\"},\"option\":{\"title\":{\"text\":\"年度销售额趋势\",\"subtext\":\"虚拟数据\",\"left\":\"center\"},\"tooltip\":{\"trigger\":\"axis\"},\"legend\":{\"data\":[\"线上销售\",\"线下销售\"],\"top\":\"5%\",\"left\":\"center\"},\"xAxis\":{\"type\":\"category\",\"boundaryGap\":false,\"data\":[\"一月\",\"二月\",\"三月\",\"四月\",\"五月\",\"六月\",\"七月\",\"八月\",\"九月\",\"十月\",\"十一月\",\"十二月\"]},\"yAxis\":{\"type\":\"value\"},\"series\":[{\"name\":\"线上销售\",\"type\":\"line\",\"data\":[120,132,101,134,90,230,210,182,191,234,290,330],\"markPoint\":{\"data\":[{\"type\":\"max\",\"name\":\"最大值\"},{\"type\":\"min\",\"name\":\"最小值\"}]},\"markLine\":{\"data\":[{\"type\":\"average\",\"name\":\"平均值\"}]}},{\"name\":\"线下销售\",\"type\":\"line\",\"data\":[220,182,191,234,290,330,310,123,178,165,189,191],\"markPoint\":{\"data\":[{\"type\":\"max\",\"name\":\"最大值\"},{\"type\":\"min\",\"name\":\"最小值\"}]},\"markLine\":{\"data\":[{\"type\":\"average\",\"name\":\"平均值\"}]}}],\"color\":[\"#5470C6\",\"#EE6666\"]}}";
        String formRender = "<echarts_render>" + formSetting + "</echarts_render>";
        return new NodeResult(Map.of("result", formRender, "answer", formRender,
                "form_field_list", "",
                "form_content_format", ""), Map.of());

    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer", context.get("answer"));
        detail.put("branch_id", context.get("branch_id"));
        detail.put("branch_name", context.get("branch_name"));
        return detail;
    }
}
