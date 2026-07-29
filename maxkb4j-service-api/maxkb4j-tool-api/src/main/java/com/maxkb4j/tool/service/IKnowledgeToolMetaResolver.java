package com.maxkb4j.tool.service;

import com.maxkb4j.tool.vo.ToolRenderMeta;

/**
 * 解析 agent 类工具调用展示元数据的 SPI。
 *
 * <p>agent 工具对应一个应用，其图标/名称需查询应用数据，
 * 此查询本质上属于 application 领域。接口声明在 tool-api 供
 * {@code ToolFormatterServiceImpl} 消费，由 application 模块实现并在运行期注入，
 * 避免 tool 模块反向编译依赖 application。
 *
 * @author tarzan
 */
public interface IKnowledgeToolMetaResolver {

    /**
     * 按 agent（应用）ID 解析展示元数据。
     *
     * @param agentId 应用 ID
     * @return 展示元数据；不存在时返回 null，由调用方走 fallback
     */
    ToolRenderMeta resolve(String agentId);
}
