package io.github.matheuslealpa.apicore.it;

import com.jayway.jsonpath.JsonPath;
import io.github.matheuslealpa.apicore.test.ApiCoreJwt;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
@AutoConfigureMockMvc
class ApiCoreJwtTests {

    @Autowired
    MockMvc mvc;

    @Test
    void rolesViramAuthoritiesComPrefixo() throws Exception {
        String body = mvc.perform(post("/catalog").with(ApiCoreJwt.jwtWithRoles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"jwt-helper\",\"price\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(delete("/catalog/{id}", id).with(ApiCoreJwt.jwtWithRoles("USER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/catalog/{id}", id).with(ApiCoreJwt.jwtWithRoles("ADMIN"))).andExpect(status().isNoContent());
    }

    @Test
    void claimAninhadoNoFormatoDoKeycloak() throws Exception {
        mvc.perform(get("/products/me").with(ApiCoreJwt.jwtWithClaimRoles("realm_access.roles", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claims.realm_access.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"));
    }
}
