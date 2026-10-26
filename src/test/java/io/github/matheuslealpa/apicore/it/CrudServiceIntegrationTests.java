package io.github.matheuslealpa.apicore.it;

import java.util.UUID;

import io.github.matheuslealpa.apicore.exception.BusinessException;
import io.github.matheuslealpa.apicore.exception.ResourceNotFoundException;
import io.github.perplexhub.rsql.RSQLJPASupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.threads.virtual.enabled=false")
class CrudServiceIntegrationTests {

    @Autowired
    ProductService service;

    @Autowired
    ProductRepository repository;

    @BeforeEach
    void limpa() {
        repository.deleteAll();
    }

    @Test
    void criaEBuscaPorId() {
        Product criado = service.create(Product.builder().name("crud-teclado").price(150).build());

        assertThat(criado.getId()).isNotNull();
        assertThat(service.findById(criado.getId()).getName()).isEqualTo("crud-teclado");
        assertThat(service.existsById(criado.getId())).isTrue();
    }

    @Test
    void listaPaginadoEComFiltro() {
        service.create(Product.builder().name("crud-a").price(10).build());
        service.create(Product.builder().name("crud-b").price(200).build());
        PageRequest pagina = PageRequest.of(0, 10, Sort.by("name"));

        assertThat(service.findAll(pagina).getTotalElements()).isEqualTo(2);
        Page<Product> filtrado = service.findAll(RSQLJPASupport.toSpecification("price=gt=100"), pagina);
        assertThat(filtrado.getContent()).extracting(Product::getName).containsExactly("crud-b");
    }

    @Test
    void atualizaAplicandoAlteracoes() {
        UUID id = service.create(Product.builder().name("crud-velho").build()).getId();

        Product atualizado = service.update(id, product -> product.setName("crud-novo"));

        assertThat(atualizado.getName()).isEqualTo("crud-novo");
        assertThat(service.findById(id).getName()).isEqualTo("crud-novo");
    }

    @Test
    void removeERetorna404DepoisDisso() {
        UUID id = service.create(Product.builder().name("crud-remover").build()).getId();

        service.delete(id);

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void hookBloqueiaRemocao() {
        UUID id = service.create(Product.builder().name("protegido-1").build()).getId();

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(BusinessException.class);
        assertThat(service.existsById(id)).isTrue();
    }
}
