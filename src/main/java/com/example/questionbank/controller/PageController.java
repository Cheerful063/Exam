package com.example.questionbank.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/upload")
    public String upload() {
        return "forward:/upload.html";
    }

    @GetMapping("/practice")
    public String practice() {
        return "forward:/practice.html";
    }
}