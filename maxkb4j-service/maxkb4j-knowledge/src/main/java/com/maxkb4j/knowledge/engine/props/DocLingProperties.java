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
     * 是否启用 OCR
     */
    private boolean enableOcr = true;

    /**
     * 是否解析表格
     */
    private boolean enableTable = true;
    /**
     * 是否解析代码
     */
    private boolean enableCode = false;

    /**
     * 是否解析公式
     */
    private boolean enableFormula = false;

    /**
     * HTTP 调用超时时间，单位秒（整体超时，含轮询等待）
     */
    private long timeout = 600;

    /**
     * MinerU 支持解析的文件扩展名（小写，含点号）
     */
    private List<String> supportedExtensions = List.of(
            ".pdf",  ".docx", ".pptx", ".xlsx", ".csv", ".html"
    );
}
