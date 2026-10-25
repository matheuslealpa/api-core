package com.github.matheuslealpa.apicore.it;

import java.util.UUID;

import com.github.matheuslealpa.apicore.it.TestApplication.ProductCatalog;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
@AutoConfigureMockMvc
class ApiCoreIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository repository;

    @Autowired
    ProductCatalog catalog;

    @Autowired
    Environment environment;

    @Test
    void healthEPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void prometheusExpostoComMetricasDeCache() throws Exception {
        mvc.perform(get("/actuator/prometheus").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("cache_gets")));
    }

    @Test
    void semTokenRetorna401ComProblemDetail() throws Exception {
        mvc.perform(get("/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void criaComAuditoriaDoUsuarioDoJwt() throws Exception {
        mvc.perform(post("/products")
                        .with(jwt().jwt(token -> token.subject("123").claim("preferred_username", "maria")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Teclado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdBy").value("maria"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void validacaoRetornaProblemDetailComErros() throws Exception {
        mvc.perform(post("/products").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void listaPaginada() throws Exception {
        repository.save(new Product("Mouse"));
        mvc.perform(get("/products?size=1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void cacheEvitaSegundaConsulta() throws Exception {
        UUID id = repository.save(new Product("Monitor")).getId();
        int before = catalog.lookups();
        mvc.perform(get("/products/{id}/name", id).with(jwt())).andExpect(status().isOk());
        mvc.perform(get("/products/{id}/name", id).with(jwt())).andExpect(status().isOk());
        assertThat(catalog.lookups() - before).isEqualTo(1);
    }

    @Test
    void recursoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/products/{id}/name", UUID.randomUUID()).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void preAuthorizeSemRoleRetorna403() throws Exception {
        UUID id = repository.save(new Product("Cabo")).getId();
        mvc.perform(delete("/products/{id}", id).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void preAuthorizeComRoleAdmin() throws Exception {
        UUID id = repository.save(new Product("Hub")).getId();
        mvc.perform(delete("/products/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void defaultsAplicadosESobrescreviveis() {
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,metrics,prometheus");
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        assertThat(environment.getProperty("spring.threads.virtual.enabled")).isEqualTo("false");
    }
}
