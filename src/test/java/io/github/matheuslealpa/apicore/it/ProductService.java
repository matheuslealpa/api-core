package io.github.matheuslealpa.apicore.it;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.github.matheuslealpa.apicore.exception.BusinessException;
import io.github.matheuslealpa.apicore.service.CrudService;

import org.springframework.stereotype.Service;

@Service
class ProductService extends CrudService<Product, UUID, ProductRepository> {

    // Counts calls to the two-argument update, which CrudController must keep going through.
    private final AtomicInteger updates = new AtomicInteger();

    ProductService(ProductRepository repository) {
        super(repository);
    }

    int updates() {
        return updates.get();
    }

    @Override
    public Product update(UUID id, Consumer<? super Product> changes) {
        updates.incrementAndGet();
        return super.update(id, changes);
    }

    @Override
    protected void beforeDelete(Product product) {
        if (product.getName().startsWith("protegido")) {
            throw new BusinessException("Produto protegido não pode ser removido.");
        }
    }
}
