package com.tarzan.maxkb4j.module.knowledge.controller;

import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.knowledge.service.TemplateService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace")
@AllArgsConstructor
public class TemplateController {

    private final TemplateService templateService;


    @GetMapping("/knowledge/document/table_template/export")
    public void tableTemplateExport(String type, HttpServletResponse response) throws Exception {
        templateService.tableTemplateExport(type, response);
    }

    @GetMapping("/knowledge/document/template/export")
    public void templateExport(String type, HttpServletResponse response) throws Exception {
        templateService.templateExport(type, response);
    }
}
