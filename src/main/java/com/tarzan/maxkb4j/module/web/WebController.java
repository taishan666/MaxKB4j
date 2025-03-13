package com.tarzan.maxkb4j.module.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebController {

    @GetMapping("ui/{path:[^\\.]*}") // 匹配任何没有点的请求 (避免静态资源被拦截)
    public String forward(@PathVariable String path) {
        // 转发到 index.html
        return "forward:/ui/index.html";
    }
}
