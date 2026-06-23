package com.maxkb4j.knowledge.engine.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MinerU 文档解析引擎配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "doc-ling")
public class DocLingProperties {

    /**
     * 是否启用 MinerU 引擎，关闭时全部走本地解析
     */
    private boolean enabled = false;

    /**
     * DocLing 后端服务地址（self-hosted 模式使用），如 http://localhost:5001
     */
    private String baseUrl="http://localhost:5001";

    /**
     * MinerU API 鉴权 token（precision 模式必填）
     */
    private String apiKey;

    /**
     * 云 API 模型版本: pipeline / vlm / MinerU-HTML
     */
    private String modelVersion = "vlm";

    /**
     * 文档解析语言，如 ch / en
     */
    private String language = "ch";

    /**
     * 是否启用 OCR
     */
    private boolean enableOcr = true;

    /**
     * 是否解析表格
     */
    private boolean enableTable = true;

    /**
     * 是否解析公式
     */
    private boolean enableFormula = true;

    /**
     * 单文件最大解析页数（self-hosted 模式）
     */
    private int maxConvertPages = 500;

    /**
     * HTTP 调用超时时间，单位毫秒（整体超时，含轮询等待）
     */
    private long timeout = 600_000;

    /**
     * MinerU 支持解析的文件扩展名（小写，含点号）
     */
    private List<String> supportedExtensions = List.of(
            ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".png", ".jpg", ".jpeg"
    );
}
