package io.github.matheuslealpa.apicore.jpa;

import io.github.matheuslealpa.apicore.exception.ResourceNotFoundException;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Base repository: CRUD, pagination and {@code Specification}.
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Finds by id or throws {@link ResourceNotFoundException} (mapped to 404 by the global handler).
     */
    default T findByIdOrThrow(ID id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("Recurso não encontrado: " + id));
    }
}
