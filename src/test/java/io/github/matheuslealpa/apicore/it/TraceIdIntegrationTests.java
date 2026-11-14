package io.github.matheuslealpa.apicore.it;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
@AutoConfigureMockMvc
class TraceIdIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void respostaTrazOTraceId() throws Exception {
        mvc.perform(get("/products").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void erroDeAutenticacaoTrazOMesmoTraceIdNoHeaderENoCorpo() throws Exception {
        MvcResult result = mvc.perform(get("/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.traceId").exists())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(result.getResponse().getHeader("X-Trace-Id"));
    }

}
