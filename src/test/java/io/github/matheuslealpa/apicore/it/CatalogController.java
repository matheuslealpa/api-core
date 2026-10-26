package io.github.matheuslealpa.apicore.it;

import java.util.UUID;

import io.github.matheuslealpa.apicore.rsql.RsqlFilter;
import io.github.matheuslealpa.apicore.web.CrudController;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog")
@RsqlFilter(allow = {"name", "price"})
class CatalogController extends CrudController<Product, UUID, CatalogController.Request, CatalogController.Response> {

    record Request(@NotBlank String name, @PositiveOrZero int price) {
    }

    record Response(UUID id, String name, int price) {
    }

    CatalogController(ProductService service) {
        super(service);
    }

    // Mapping and @PathVariable are inherited from CrudController; only the access rule is added.
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID id) {
        super.delete(id);
    }

    @Override
    protected Response toResponse(Product product) {
        return new Response(product.getId(), product.getName(), product.getPrice());
    }

    @Override
    protected Product toEntity(Request request) {
        return Product.builder().name(request.name()).price(request.price()).build();
    }

    @Override
    protected void applyChanges(Product product, Request request) {
        product.setName(request.name());
        product.setPrice(request.price());
    }
}
