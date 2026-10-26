package io.github.matheuslealpa.apicore.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
@AutoConfigureMockMvc
class RsqlFilterIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.save(new Product("rsql-teclado", 150));
        repository.save(new Product("rsql-teclado-mini", 80));
        repository.save(new Product("rsql-mouse", 200));
    }

    @Test
    void filtraPorWildcardEComparacao() throws Exception {
        mvc.perform(get("/products/search").param("filter", "name==rsql-tecl*;price=gt=100").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(hasItem("rsql-teclado")))
                .andExpect(jsonPath("$.content[*].name").value(not(hasItem("rsql-teclado-mini"))))
                .andExpect(jsonPath("$.content[*].name").value(not(hasItem("rsql-mouse"))));
    }

    @Test
    void filtraComIn() throws Exception {
        mvc.perform(get("/products/search").param("filter", "name=in=(rsql-mouse,rsql-teclado-mini)").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(containsInAnyOrder("rsql-mouse", "rsql-teclado-mini")));
    }

    @Test
    void semFiltroNaoRestringe() throws Exception {
        mvc.perform(get("/products/search").param("size", "100").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(hasItem("rsql-mouse")));
    }

    @Test
    void propriedadeForaDaWhitelistRetorna400() throws Exception {
        mvc.perform(get("/products/search").param("filter", "createdBy==maria").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.property").value("createdBy"));
    }

    @Test
    void sintaxeInvalidaRetorna400() throws Exception {
        mvc.perform(get("/products/search").param("filter", "name==").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Filtro com sintaxe inválida."));
    }

    @Test
    void valorIncompativelComOTipoRetorna400() throws Exception {
        mvc.perform(get("/products/search").param("filter", "price=gt=abc").with(jwt()))
                .andExpect(status().isBadRequest());
    }
}
