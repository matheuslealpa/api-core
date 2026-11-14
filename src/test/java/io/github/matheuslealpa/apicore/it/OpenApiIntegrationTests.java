package io.github.matheuslealpa.apicore.it;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
@AutoConfigureMockMvc
class OpenApiIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void listagemDocumentaFiltroEPaginacao() throws Exception {
        mvc.perform(get("/v3/api-docs").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/catalog'].get.parameters[*].name",
                        containsInAnyOrder("filter", "page", "size", "sort")))
                .andExpect(jsonPath("$.paths['/catalog'].get.parameters[?(@.name == 'filter')].description",
                        hasItem(containsString("properties: name, price."))))
                .andExpect(jsonPath("$.paths['/products'].get.parameters[*].name", not(hasItem("pageable"))));
    }

    @Test
    void registraEsquemaJwt() throws Exception {
        mvc.perform(get("/v3/api-docs").with(jwt()))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearer-jwt").isArray());
    }
}
