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
/*
    @GetMapping({"/ui/{path:[^.]*}"}) // 匹配任何没有点的请求 (避免静态资源被拦截)
    public String forward(@PathVariable String path) {
        System.out.println(path);
        // 转发到 index.html
        return "forward:/ui/index.html";
    }

    @GetMapping({"/ui/{path:[^.]*}/{path1:[^.]*}/{path2:[^.]*}"}) // 匹配任何没有点的请求 (避免静态资源被拦截)
    public String forward(@PathVariable String path,@PathVariable String path1,@PathVariable String path2) {
        System.out.println(path+" "+path1+" "+path2);
        // 转发到 index.html
        return "forward:/ui/index.html";
    }

    @GetMapping({"/ui/{path:[^.]*}/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}"}) // 匹配任何没有点的请求 (避免静态资源被拦截)
    public String forward(@PathVariable String path,@PathVariable String path1,@PathVariable String path2,@PathVariable String path3) {
        System.out.println(path+" "+path1+" "+path2+" "+path3);
        // 转发到 index.html
        return "forward:/ui/index.html";
    }*/


}
