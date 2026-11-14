package io.github.matheuslealpa.apicore.it;

import java.util.Map;
import java.util.UUID;

import io.github.matheuslealpa.apicore.exception.BusinessException;
import io.github.matheuslealpa.apicore.web.CrudController;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patchable")
class PatchableCatalogController
        extends CrudController<Product, UUID, CatalogController.Request, CatalogController.Response> {

    PatchableCatalogController(ProductService service) {
        super(service);
    }

    @Override
    protected CatalogController.Response toResponse(Product product) {
        return new CatalogController.Response(product.getId(), product.getName(), product.getPrice());
    }

    @Override
    protected Product toEntity(CatalogController.Request request) {
        return Product.builder().name(request.name()).price(request.price()).build();
    }

    @Override
    protected void applyChanges(Product product, CatalogController.Request request) {
        product.setName(request.name());
        product.setPrice(request.price());
    }

    @Override
    protected void applyPatch(Product product, Map<String, Object> patch) {
        if (patch.containsKey("price")) {
            if (!(patch.get("price") instanceof Integer price) || price < 0) {
                throw new BusinessException("price must be a non-negative integer.");
            }
            product.setPrice(price);
        }
    }
}
