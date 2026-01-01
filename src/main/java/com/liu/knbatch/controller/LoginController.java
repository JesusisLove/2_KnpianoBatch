package com.liu.knbatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class LoginController {

    @PostMapping(value = "/user/login")
    public String login(@RequestParam("username") String username, // 明确地指定是从request里获取username参数
                        @RequestParam("password") String password, // 明确地指定是从request里获取password参数
                        Map<String,Object> map,
                        HttpSession session // 2020/07/28 为了使用拦截器
                        ) {

        /* 2020/08/04 学习错误信息处理机制 自定义异常处理应用 开始 */
        if (!username.equals("admin")) {
            /*
            // 如果用户不是admin，则抛自定义的异常
             throw new UserNotExistException();
            */
            map.put("msg","用户名密码错误");
            return "login";
        }
        /* 2020/08/04 学习错误信息处理机制 自定义异常处理应用 终了 */


        if (!StringUtils.isEmpty(username) && "1".equals(password)) {
            /* 2020/07/28 登录成功，跳转到dashboard画面 改进版
            * 为了防止刷新表单，避免重复提交表单，解决的办法是重定向
            * 登录成功的页面需要模版引擎的解析，所以需要在MyMvcConfig.java里加一个视图映射
            * */
            //当点击登录按钮进入dashboard页面后，为了防止重复提交表单可以重定向到该页面
            session.setAttribute("loginUser",username);
            return "redirect:/main.html";

        } else {
            // 登录失败，停留在当前画面，报错误信息
            map.put("msg","用户名密码错误");
            return "login";
        }
    }
}
