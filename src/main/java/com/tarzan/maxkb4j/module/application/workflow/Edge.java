package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class Edge {
    private String id;
    private String type;
    private String sourceNodeId;
    private String targetNodeId;
    private Point startPoint;
    private Point endPoint;
    private List<Point> pointsList;
    private JSONObject properties;

}
@Data
class Point{
    private Float x;
    private Float y;
}
