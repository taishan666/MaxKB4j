package com.tarzan.maxkb4j.core.workflow.node.echarts.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.echarts.input.EchartsNodeParams;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.ECHARTS;

public class BaseEchartsNode extends INode {


    public BaseEchartsNode(JSONObject properties) {
        super(properties);
    }

    @Override
    public NodeResult execute() {
        System.out.println(ECHARTS);
        EchartsNodeParams nodeParams = super.nodeParams.toJavaObject(EchartsNodeParams.class);
        Object title = this.workflowManage.getFieldValue(nodeParams.getTitleType(), nodeParams.getTitle(), nodeParams.getTitleReference());
        String xAxisStr = (String) this.workflowManage.getFieldValue(nodeParams.getXAxisType(), nodeParams.getXAxis(), nodeParams.getXAxisReference());
        String yAxisStr = (String) this.workflowManage.getFieldValue(nodeParams.getYAxisType(), nodeParams.getYAxis(), nodeParams.getYAxisReference());
        //line-bar-pie
        String chartType = (String) this.workflowManage.getFieldValue(nodeParams.getChartTypeType(), nodeParams.getChartType(), nodeParams.getChartTypeReference());
        JSONArray xAxis = JSONArray.parseArray(xAxisStr);
        JSONArray yAxis = JSONArray.parseArray(yAxisStr);
        JSONObject formSetting = getEcharts(chartType, title, xAxis, yAxis);
        String formRender = "<echarts_render>" + formSetting.toJSONString() + "</echarts_render>";
        return new NodeResult(Map.of("result", formRender, "answer", formRender), Map.of());

    }


    private JSONObject getEcharts(String chartType, Object chartTitle, JSONArray xAxisData, JSONArray yAxisData) {
        JSONObject style = new JSONObject();
        style.put("height", "400px");
        style.put("width", "100%");

        // 构建 title 对象
        JSONObject title = new JSONObject();
        title.put("text", chartTitle);
        title.put("left", "center");

        // 构建 tooltip 对象
        JSONObject tooltip = new JSONObject();


        // 构建 xAxis 对象
        JSONObject xAxis = new JSONObject();
        xAxis.put("type", "category");
        xAxis.put("boundaryGap", false);
        xAxis.put("data", xAxisData);

        // 构建 yAxis 对象
        JSONObject yAxis = new JSONObject();
        yAxis.put("type", "value");

        // 构建 markPoint 数据
        JSONObject markPointDataMax = new JSONObject();
        markPointDataMax.put("type", "max");
        markPointDataMax.put("name", "最大值");
        JSONObject markPointDataMin = new JSONObject();
        markPointDataMin.put("type", "min");
        markPointDataMin.put("name", "最小值");
        // 构建markPoint
        JSONObject onlineMarkPoint = new JSONObject();
        onlineMarkPoint.put("data", new JSONObject[]{markPointDataMax, markPointDataMin});
        // 构建线上销售 series 数据
        JSONObject series = new JSONObject();
        series.put("type", chartType);//line-bar-pie

        // 构建 option 对象
        JSONObject option = new JSONObject();
        option.put("title", title);

        // option.put("legend", legend);
        if (!"pie".equals(chartType)) {
            option.put("xAxis", xAxis);
            option.put("yAxis", yAxis);
            tooltip.put("trigger", "axis");
            series.put("data", yAxisData);
            series.put("markPoint", onlineMarkPoint);
        } else {
            JSONArray seriesData = new JSONArray();
            for (int i = 0; i < xAxisData.size(); i++) {
                String xAxisDatum = xAxisData.getString(i);
                JSONObject data = new JSONObject();
                data.put("value", yAxisData.get(i));
                data.put("name", xAxisDatum);
                seriesData.add(data);
            }
            tooltip.put("trigger", "item");
            series.put("data", seriesData);
        }
        option.put("tooltip", tooltip);
        option.put("series", new JSONObject[]{series});

        // 构建 formSetting 主对象
        JSONObject formSetting = new JSONObject();
        formSetting.put("actionType", "JSON");
        formSetting.put("style", style);
        formSetting.put("option", option);
        return formSetting;
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer", context.get("answer"));
        return detail;
    }
}
