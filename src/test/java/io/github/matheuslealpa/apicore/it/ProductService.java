package io.github.matheuslealpa.apicore.it;

import java.util.UUID;

import io.github.matheuslealpa.apicore.exception.BusinessException;
import io.github.matheuslealpa.apicore.service.CrudService;

import org.springframework.stereotype.Service;

@Service
class ProductService extends CrudService<Product, UUID, ProductRepository> {

    ProductService(ProductRepository repository) {
        super(repository);
    }

    @Override
    protected void beforeDelete(Product product) {
        if (product.getName().startsWith("protegido")) {
            throw new BusinessException("Produto protegido não pode ser removido.");
        }
    }
}
