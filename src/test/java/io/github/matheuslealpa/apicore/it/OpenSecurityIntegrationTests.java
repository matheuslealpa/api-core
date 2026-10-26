package io.github.matheuslealpa.apicore.it;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.threads.virtual.enabled=false", "api-core.security.enabled=false"})
@AutoConfigureMockMvc
class OpenSecurityIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Test
    void sobeSemJwtDecoder() {
        assertThat(context.getBeanNamesForType(JwtDecoder.class)).isEmpty();
    }

    @Test
    void rotasLiberadasSemToken() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test
    void auditoriaUsaAuditorDeSistema() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"aberto\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("system"));
    }

    @Test
    void preAuthorizeContinuaNegando() throws Exception {
        mvc.perform(delete("/products/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
    }
}
