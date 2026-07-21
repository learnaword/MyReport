package com.myreport.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 管理总页入口。
 */
@Controller
public class AdminController {

    @GetMapping({"/", "/admin"})
    public RedirectView home() {
        return new RedirectView("/admin/index.html#intro");
    }
}
