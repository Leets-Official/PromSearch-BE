package com.promsearch.commerce.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PromptAccessSecurityTest {

    @Autowired MockMvc mockMvc;

    @Test
    void anonymousUserCannotUnlockOrCopyPrompt() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/1/unlock"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/prompts/1/copy"))
                .andExpect(status().isUnauthorized());
    }
}
