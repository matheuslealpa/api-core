package com.github.matheuslealpa.apicore.jpa;

import com.github.matheuslealpa.apicore.exception.ResourceNotFoundException;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repositório base: CRUD, paginação e {@code Specification}.
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Busca por id ou lança {@link ResourceNotFoundException} (convertida em 404 pelo handler global).
     */
    default T findByIdOrThrow(ID id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("Recurso não encontrado: " + id));
    }
}
