package com.maxkb4j.tool.dto;

import com.maxkb4j.tool.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 工具调试请求 DTO：携带完整实体字段外加调试输入参数。
 *
 * <p>注意与契约层（maxkb4j-tool-api）的 {@code com.maxkb4j.tool.dto.ToolDTO} 区分，
 * 后者是 {@code IToolService} 对外契约的扁平 DTO；两者不得重名，
 * 否则同名类会在实现模块编译期遮蔽契约 DTO。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ToolDebugDTO extends ToolEntity {
    private List<ToolInputField> debugFieldList;
}
