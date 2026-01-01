package com.liu.knbatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        System.out.println("访问根路径，返回login页面");
        return "login";
    }
    
    @GetMapping("/login.html")
    public String loginHtml() {
        System.out.println("访问login.html，返回login页面");
        return "login";
    }
}