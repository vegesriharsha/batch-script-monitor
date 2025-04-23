package com.example.batchmonitor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ViewController.class)
public class ViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testIndexEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testExecutionsEndpoint() throws Exception {
        mockMvc.perform(get("/executions"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void testExecutionDetailEndpoint() throws Exception {
        mockMvc.perform(get("/executions/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}
