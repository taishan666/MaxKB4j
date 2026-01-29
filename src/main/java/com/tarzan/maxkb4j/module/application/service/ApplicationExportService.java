package com.tarzan.maxkb4j.module.application.service;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.module.application.domain.dto.MaxKb4J;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.service.ToolService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ApplicationExportService {

    private final ApplicationService applicationService;
    private final ToolService toolService;

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

    @SuppressWarnings("unchecked")
    public static List<String> getToolIdList(JSONObject workflow) {
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
                        List<String> toolIds = (List<String>) nodeData.getJSONObject("toolIds");
                        if (toolIds != null) {
                            result.addAll(toolIds);
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
