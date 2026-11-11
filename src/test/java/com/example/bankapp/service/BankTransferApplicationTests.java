package com.example.bankapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class BankTransferApplicationTests {
   
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
   
    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
   
    @Test
    void contextLoads() {
    }
   
    @Test
    void dashboardAndOperationPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("bank"));
        mockMvc.perform(get("/deposit"))
                .andExpect(status().isOk())
                .andExpect(view().name("deposit"));
        mockMvc.perform(get("/withdraw"))
                .andExpect(status().isOk())
                .andExpect(view().name("withdraw"));
    }
   
    @Test
    void largeTransfersAndAccountHistoryRender() throws Exception {
        mockMvc.perform(get("/large-transfers").param("min", "10000"))
                .andExpect(status().isOk())
                .andExpect(view().name("large-transfers"));
        mockMvc.perform(get("/accounts/100001/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"));
    }
}
