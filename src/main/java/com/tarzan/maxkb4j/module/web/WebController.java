package com.tarzan.maxkb4j.module.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {


    @GetMapping({"/","/ui"})
    public String home() {
        // 转发到 index.html
        return "forward:/ui/index.html";
    }

}
