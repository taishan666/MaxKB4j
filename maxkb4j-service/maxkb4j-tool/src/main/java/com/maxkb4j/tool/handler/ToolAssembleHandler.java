package com.maxkb4j.tool.handler;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.vo.ToolVO;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    /** 单实体装配：用于详情查询、更新返回。 */
    public ToolVO assemble(ToolEntity tool) {
        if (tool == null) {
            return null;
        }
        ToolVO vo = BeanUtil.copy(tool, ToolVO.class);
        vo.setNickname(userService.getNickname(tool.getUserId()));
        vo.setFileList(skillHandler.resolveFileList(tool));
        return vo;
    }

    /**
     * 分页装配：批量复用一次 nicknameMap 避免 N+1。fileList 仅与 SKILL 类型相关，列表场景通常无需，故置空。
     */
    public IPage<ToolVO> assemblePage(IPage<ToolEntity> page) {
        Map<String, String> nicknameMap = userService.getNicknameMap();
        return PageUtil.copy(page, entity -> {
            ToolVO vo = BeanUtil.copy(entity, ToolVO.class);
            vo.setNickname(nicknameMap.get(entity.getUserId()));
            return vo;
        });
    }
}
