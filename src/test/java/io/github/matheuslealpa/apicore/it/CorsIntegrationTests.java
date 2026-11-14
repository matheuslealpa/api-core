package io.github.matheuslealpa.apicore.it;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.threads.virtual.enabled=false",
        "api-core.security.cors.allowed-origins=https://*.example.com"})
@AutoConfigureMockMvc
class CorsIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void preflightDeOrigemLiberada() throws Exception {
        mvc.perform(options("/products")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.com"));
    }

    @Test
    void origemNaoLiberadaRecusada() throws Exception {
        mvc.perform(get("/products").with(jwt()).header("Origin", "https://evil.test"))
                .andExpect(status().isForbidden());
    }

}
