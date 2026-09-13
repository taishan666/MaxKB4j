package com.maxkb4j.tool.handler;

import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.vo.ToolVO;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工具 VO 装配处理器：统一 {@link ToolEntity} 到 {@link ToolVO} 的补充字段（nickname / fileList）。
 *
 * @author tarzan
 */
@Component
@RequiredArgsConstructor
public class ToolAssembleHandler {

    private final IUserService userService;
    private final ToolSkillHandler skillHandler;

    /**
     * 单实体装配：用于详情查询、更新返回。
     */
    public ToolVO assemble(ToolEntity tool) {
        if (tool == null) {
            return null;
        }
        ToolVO vo = BeanUtil.copy(tool, ToolVO.class);
        vo.setNickname(userService.getNickname(tool.getUserId()));
        vo.setFileList(skillHandler.resolveFileList(tool));
        return vo;
    }

}
