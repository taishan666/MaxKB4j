package com.maxkb4j.application.service;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.MaxKb4J;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.workflow.enums.NodeType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ApplicationExportService {

    private final ApplicationService applicationService;
    private final IToolService toolService;

    public void appExport(String id, HttpServletResponse response) throws IOException {
        ApplicationEntity app = applicationService.getById(id);
        List<String> toolIds = new ArrayList<>();
        if (app.getToolIds()!= null){
            toolIds.addAll(app.getToolIds());
        }
        toolIds.addAll(getToolIdList(app.getWorkFlow()));
        List<ToolEntity> toolList=new ArrayList<>();
        if (!toolIds.isEmpty()){
            toolList=toolService.listByIds(toolIds);
        }
        MaxKb4J maxKb4J = new MaxKb4J(app, toolList, "v2");
        byte[] bytes = JSONUtil.toJsonStr(maxKb4J).getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(app.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".mk");
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
    }

    private static List<String> getToolIdList(JSONObject workflow) {
        List<String> result = new ArrayList<>();
        if (workflow == null) {
            return result;
        }
        JSONArray nodes = workflow.getJSONArray("nodes");
        if (nodes == null) {
            return result;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node= nodes.getJSONObject(i);
            if (node == null) continue;
            String type = node.getString("type");
            if (NodeType.TOOL_LIB.getKey().equals(type)) {
                JSONObject properties = node.getJSONObject("properties");
                if (properties != null) {
                    JSONObject nodeData = properties.getJSONObject("nodeData");
                    if (nodeData != null) {
                        String toolId = nodeData.getString("toolLibId");
                        if (toolId != null && !toolId.isEmpty()) {
                            result.add(toolId);
                        }
                    }
                }
            } else if (NodeType.LOOP.getKey().equals(type)) {
                JSONObject properties = node.getJSONObject("properties");
                if (properties != null) {
                    JSONObject nodeData = properties.getJSONObject("nodeData");
                    if (nodeData != null) {
                        JSONObject loopBody = nodeData.getJSONObject("loopBody");
                        if (loopBody != null) {
                            result.addAll(getToolIdList(loopBody));
                        }
                    }
                }
            } else if (NodeType.AI_CHAT.getKey().equals(type)) {
                JSONObject properties = node.getJSONObject("properties");
                if (properties != null) {
                    JSONObject nodeData = properties.getJSONObject("nodeData");
                    if (nodeData != null) {
                        JSONArray toolIds =  nodeData.getJSONArray("toolIds");
                        if (toolIds == null) continue;
                        for (Object toolId : toolIds) {
                            result.add(toolId.toString());
                        }
                    }
                }
            } else if (NodeType.MCP.getKey().equals(type)) {
                JSONObject properties = node.getJSONObject("properties");
                if (properties != null) {
                    JSONObject nodeData = properties.getJSONObject("nodeData");
                    if (nodeData != null) {
                        String mcpToolId = nodeData.getString("mcpToolId");
                        if (mcpToolId != null && !mcpToolId.isEmpty()) {
                            result.add(mcpToolId);
                        }
                    }
                }
            }
        }
        return result;
    }

}
