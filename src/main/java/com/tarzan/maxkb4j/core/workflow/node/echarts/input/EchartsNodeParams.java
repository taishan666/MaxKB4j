package com.tarzan.maxkb4j.core.workflow.node.echarts.input;

import lombok.Data;

import java.util.List;

@Data
public class EchartsNodeParams {
    private String titleType;
    private String title;
    private List<String> titleReference;
    private String xAxisType;
    private String xAxis;
    private List<String> xAxisReference;
    private String yAxisType;
    private String yAxis;
    private List<String> yAxisReference;
    private String chartTypeType;
    private String chartType;
    private List<String> chartTypeReference;
    private Boolean isResult;

}
