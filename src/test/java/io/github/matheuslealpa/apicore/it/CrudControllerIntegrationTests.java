package io.github.matheuslealpa.apicore.it;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
@AutoConfigureMockMvc
class CrudControllerIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void limpa() {
        repository.deleteAll();
    }

    @Test
    void criaBuscaAtualizaERemove() throws Exception {
        String id = criar("{\"name\":\"catalogo-teclado\",\"price\":150}");

        mvc.perform(get("/catalog/{id}", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("catalogo-teclado"));

        mvc.perform(put("/catalog/{id}", id).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"catalogo-mouse\",\"price\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("catalogo-mouse"))
                .andExpect(jsonPath("$.price").value(90));

        mvc.perform(delete("/catalog/{id}", id).with(jwt())).andExpect(status().isForbidden());
        mvc.perform(delete("/catalog/{id}", id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/catalog/{id}", id).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void validaCorpoDaRequisicao() throws Exception {
        mvc.perform(post("/catalog").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    void idInexistenteOuInvalido() throws Exception {
        mvc.perform(get("/catalog/{id}", UUID.randomUUID()).with(jwt())).andExpect(status().isNotFound());
        mvc.perform(put("/catalog/{id}", UUID.randomUUID()).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"price\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/catalog/nao-e-uuid").with(jwt())).andExpect(status().isBadRequest());
    }

    @Test
    void listaComFiltroEWhitelistDaClasse() throws Exception {
        criar("{\"name\":\"catalogo-barato\",\"price\":10}");
        criar("{\"name\":\"catalogo-caro\",\"price\":500}");

        mvc.perform(get("/catalog").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(get("/catalog").param("filter", "price=gt=100").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(org.hamcrest.Matchers.contains("catalogo-caro")));

        mvc.perform(get("/catalog").param("filter", "createdBy==alguem").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.property").value("createdBy"));
    }

    @Test
    void sortSegueAWhitelistDoFiltro() throws Exception {
        criar("{\"name\":\"catalogo-a\",\"price\":10}");
        criar("{\"name\":\"catalogo-b\",\"price\":20}");

        mvc.perform(get("/catalog").param("sort", "price,desc").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("catalogo-b"));

        mvc.perform(get("/catalog").param("sort", "createdBy").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.property").value("createdBy"));

        // Default whitelist (basic attributes): collections cannot be used to sort either.
        mvc.perform(get("/products/auto").param("sort", "tags,asc").with(jwt()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/products/any").param("sort", "name").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void etagEIfMatchEvitamLostUpdate() throws Exception {
        String id = criar("{\"name\":\"catalogo-etag\",\"price\":10}");

        mvc.perform(get("/catalog/{id}", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""));

        mvc.perform(put("/catalog/{id}", id).with(jwt()).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"primeiro\",\"price\":11}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""));

        // Second client still holds version 0.
        mvc.perform(put("/catalog/{id}", id).with(jwt()).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"segundo\",\"price\":12}"))
                .andExpect(status().isPreconditionFailed());

        mvc.perform(put("/catalog/{id}", id).with(jwt()).header("If-Match", "*")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"terceiro\",\"price\":13}"))
                .andExpect(status().isOk());

        mvc.perform(get("/catalog/{id}", id).with(jwt())).andExpect(jsonPath("$.name").value("terceiro"));
    }

    @Test
    void patchSoQuandoASubclasseImplementa() throws Exception {
        String id = criar("{\"name\":\"catalogo-patch\",\"price\":10}");

        mvc.perform(patch("/catalog/{id}", id).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":99}"))
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(patch("/patchable/{id}", id).with(jwt()).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("catalogo-patch"))
                .andExpect(jsonPath("$.price").value(99))
                .andExpect(header().string("ETag", "\"1\""));

        mvc.perform(patch("/patchable/{id}", id).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":-5}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void exigeAutenticacao() throws Exception {
        mvc.perform(get("/catalog")).andExpect(status().isUnauthorized());
    }

    private String criar(String body) throws Exception {
        String response = mvc.perform(post("/catalog").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
