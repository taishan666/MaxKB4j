package com.tarzan.maxkb4j.module.system.shared.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolQuery;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.domain.vo.ToolVO;
import com.tarzan.maxkb4j.module.tool.service.ToolService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(AppConst.ADMIN_API+"/system/shared/workspace/default")
@AllArgsConstructor
public class SharedController {

    private final ToolService toolService;
    private final KnowledgeService knowledgeService;

    @GetMapping("/knowledge/{current}/{size}")
    public R<IPage<KnowledgeVO>> knowledgePage(@PathVariable("current") int current, @PathVariable("size") int size, KnowledgeQuery query) {
        Page<KnowledgeVO> knowledgePage = new Page<>(current, size);
        return R.success(knowledgeService.selectKnowledgePage(knowledgePage, query));
    }

    @GetMapping("/tool/{current}/{size}")
    public R<IPage<ToolVO>> toolPage(@PathVariable int current, @PathVariable int size, ToolQuery query) {
        return R.success(toolService.pageList(current, size, query));
    }

    @GetMapping("/knowledge")
    public R<List<KnowledgeEntity>> listDatasets() {
        return R.success(knowledgeService.list(StpUtil.getLoginIdAsString(),"shared"));
    }

    @GetMapping("/tool")
    public R<Map<String, List<ToolEntity>>> toolList() {
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ToolEntity::getIsActive, true);
        wrapper.eq(ToolEntity::getFolderId, "shared");
        List<ToolEntity> tools = toolService.list(wrapper);
        return R.success(Map.of("folders", List.of(), "tools", tools));
    }



}
