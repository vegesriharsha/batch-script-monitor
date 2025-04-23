package com.example.batchmonitor.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/executions")
    public String executions() {
        return "index";
    }

    @GetMapping("/executions/{id}")
    public String executionDetail() {
        return "index";
    }
}
