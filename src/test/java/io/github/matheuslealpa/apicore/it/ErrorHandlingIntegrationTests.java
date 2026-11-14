package io.github.matheuslealpa.apicore.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
@AutoConfigureMockMvc
class ErrorHandlingIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void limpa() {
        repository.deleteAll();
    }

    @Test
    void constraintDuplicadaRetorna409SemVazarSql() throws Exception {
        mvc.perform(post("/products/sku/{sku}", "ABC-1").with(jwt())).andExpect(status().isCreated());

        mvc.perform(post("/products/sku/{sku}", "ABC-1").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data."));
    }

    @Test
    void sortPorPropriedadeInexistenteRetorna400() throws Exception {
        mvc.perform(get("/products").param("sort", "naoExiste").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.property").value("naoExiste"));
    }

    @Test
    void excecaoComResponseStatusMantemOStatus() throws Exception {
        mvc.perform(get("/products/teapot").with(jwt()))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.status").value(418))
                .andExpect(jsonPath("$.detail").value("No coffee here."));
    }
}
