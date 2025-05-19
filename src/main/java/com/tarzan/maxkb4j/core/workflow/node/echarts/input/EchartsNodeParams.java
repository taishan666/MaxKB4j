package com.tarzan.maxkb4j.core.workflow.node.echarts.input;

import lombok.Data;

import java.util.List;

@Data
public class EchartsNodeParams {
    private String title;
    private String subTitle;
    private List<String> xAxis;
    private List<Object> yAxis;
    private String chartType;

}
