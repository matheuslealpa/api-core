package io.github.matheuslealpa.apicore.it;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LombokSuperBuilderTests {

    @Test
    void builderExpoeSoCamposDaSubclasse() {
        Product product = Product.builder().name("Teclado").price(150).build();

        assertThat(product.getName()).isEqualTo("Teclado");
        assertThat(product.getPrice()).isEqualTo(150);
        assertThat(product.getId()).isNull();
        assertThat(product.getVersion()).isNull();
    }

    @Test
    void builderNaoExpoeCamposGerenciadosPeloJpa() {
        assertThat(Arrays.stream(Product.builder().getClass().getMethods()).map(Method::getName))
                .doesNotContain("id", "version", "createdAt", "createdBy", "updatedAt", "updatedBy");
    }
}
