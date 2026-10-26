package io.github.matheuslealpa.apicore.it;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.matheuslealpa.apicore.jpa.PageResponse;
import io.github.matheuslealpa.apicore.rsql.RsqlFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
class TestApplication {

    // Os testes autenticam com jwt() do spring-security-test; tokens reais são rejeitados.
    @Bean
    JwtDecoder jwtDecoder() {
        return token -> {
            throw new BadJwtException("token inválido");
        };
    }

    record ProductRequest(@NotBlank String name) {
    }

    @Service
    static class ProductCatalog {

        private final AtomicInteger lookups = new AtomicInteger();

        private final ProductRepository repository;

        ProductCatalog(ProductRepository repository) {
            this.repository = repository;
        }

        public int lookups() {
            return lookups.get();
        }

        @Cacheable("products")
        public String name(UUID id) {
            lookups.incrementAndGet();
            return repository.findByIdOrThrow(id).getName();
        }
    }

    @RestController
    @RequestMapping("/products")
    static class ProductController {

        private final ProductRepository repository;

        private final ProductCatalog catalog;

        ProductController(ProductRepository repository, ProductCatalog catalog) {
            this.repository = repository;
            this.catalog = catalog;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        Product create(@Valid @RequestBody ProductRequest request) {
            return repository.save(new Product(request.name()));
        }

        @GetMapping
        PageResponse<Product> list(Pageable pageable) {
            return PageResponse.of(repository.findAll(pageable));
        }

        @GetMapping("/search")
        PageResponse<Product> search(@RsqlFilter(allow = {"name", "price"}) Specification<Product> filter,
                Pageable pageable) {
            return PageResponse.of(repository.findAll(filter, pageable));
        }

        @GetMapping("/{id}/name")
        String name(@PathVariable UUID id) {
            return catalog.name(id);
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void delete(@PathVariable UUID id) {
            repository.delete(repository.findByIdOrThrow(id));
        }
    }
}
