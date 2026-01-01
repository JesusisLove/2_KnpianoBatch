package com.liu.knbatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主菜单控制器
 */
@Controller
public class MainController {

    /**
     * 主菜单页面
     */
    @GetMapping("/main.html")
    public String main() {
        return "main";  // 返回main.html模板
    }
    
    /**
     * 仪表板页面
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}