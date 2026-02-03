package com.tarzan.maxkb4j.module.tool.handler;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import dev.langchain4j.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具连接测试处理器
 *
 * @author tarzan
 */
@Slf4j
@Component
public class ToolConnectionHandler {

    /**
     * 测试MCP服务器连接
     *
     * @param code MCP服务器配置代码
     * @return 连接是否成功
     */
    public boolean testConnection(String code) {
        JSONObject serverConfig = JSONObject.parseObject(code);
        if (serverConfig == null || serverConfig.isEmpty()) {
            return false;
        }
        List<McpClient> mcpClients = McpToolUtil.getMcpClients(serverConfig);
        for (McpClient mcpClient : mcpClients) {
            try {
                mcpClient.checkHealth();
                return true;
            } catch (Exception e) {
                log.warn("MCP服务器连接测试失败", e);
            } finally {
                try {
                    mcpClient.close();
                } catch (Exception e) {
                    log.warn("关闭MCP客户端时发生错误", e);
                }
            }
        }
        return false;
    }
}