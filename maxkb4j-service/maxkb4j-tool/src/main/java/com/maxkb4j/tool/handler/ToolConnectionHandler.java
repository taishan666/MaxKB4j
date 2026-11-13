package com.maxkb4j.tool.handler;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.util.McpToolUtil;
import dev.langchain4j.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        McpClient mcpClient = null;
        try {
            mcpClient = McpToolUtil.getMcpClient(serverConfig);
            if (mcpClient != null) {
                mcpClient.checkHealth();
                return true;
            }
        } catch (Exception e) {
            log.warn("MCP服务器连接测试失败", e);
            if (mcpClient != null) {
                // 客户端由 McpToolUtil 缓存复用，此处不能 close——那会关闭缓存中的
                // 共享实例且缓存条目仍在，导致相同配置的后续测试/调用一直报
                // "The client is closed"；健康检查失败时驱逐即可，下次访问将重建连接
                McpToolUtil.evict(mcpClient);
            }
        }
        return false;
    }
}