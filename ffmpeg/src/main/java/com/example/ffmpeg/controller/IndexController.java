package com.example.ffmpeg.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/")
public class IndexController {

    @GetMapping("/index")
    public String home(HttpServletRequest request, Model model) {
        return "index";
    }

    @GetMapping("/test")
    public String test(HttpServletRequest request, Model model) {
        return "test";
    }

    @GetMapping("/test2")
    public String test2(HttpServletRequest request, Model model) {
        return "test2";
    }

    @GetMapping("/upload")
    public String upload(HttpServletRequest request, Model model) {
        return "upload";
    }

    @GetMapping("/watch")
    public String watch(HttpServletRequest request, Model model) {
        return "watch";
    }

    @GetMapping("/video")
    public String video(HttpServletRequest request, Model model) {
        model.addAttribute("date", request.getParameter("date"));
        model.addAttribute("videoName", request.getParameter("videoName"));
        return "video";
    }

}
