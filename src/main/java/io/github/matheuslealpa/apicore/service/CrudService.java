package io.github.matheuslealpa.apicore.service;

import io.github.matheuslealpa.apicore.exception.PreconditionFailedException;
import io.github.matheuslealpa.apicore.exception.ResourceNotFoundException;
import io.github.matheuslealpa.apicore.jpa.AuditableEntity;
import io.github.matheuslealpa.apicore.jpa.BaseRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * Base service with CRUD operations. Reads run in read-only transactions; missing ids raise
 * {@link ResourceNotFoundException} (404). Override the {@code before*} hooks to add business rules.
 *
 * <pre>{@code
 * @Service
 * public class UserService extends CrudService<User, UUID, UserRepository> {
 *     public UserService(UserRepository repository) {
 *         super(repository);
 *     }
 * }
 * }</pre>
 *
 * @param <T>  entity type
 * @param <ID> id type
 * @param <R>  repository type, exposed to subclasses as {@link #repository}
 */
@Transactional(readOnly = true)
public abstract class CrudService<T, ID, R extends BaseRepository<T, ID>> {

    protected final R repository;

    protected CrudService(R repository) {
        this.repository = repository;
    }

    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Lists entities matching the specification, e.g. one resolved from {@code @RsqlFilter}.
     */
    public Page<T> findAll(Specification<T> specification, Pageable pageable) {
        return repository.findAll(specification, pageable);
    }

    public T findById(ID id) {
        return repository.findByIdOrThrow(id);
    }

    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

    @Transactional
    public T create(T entity) {
        beforeCreate(entity);
        return repository.save(entity);
    }

    /**
     * Loads the entity, applies the changes and saves it.
     */
    @Transactional
    public T update(ID id, Consumer<? super T> changes) {
        return update(id, null, changes);
    }

    /**
     * Loads the entity, checks that it is still at {@code expectedVersion}, applies the changes and saves it.
     * Prevents lost updates between clients: the version is the one the client read, e.g. from an {@code ETag}.
     *
     * @param expectedVersion version the client read; {@code null} skips the check
     * @throws PreconditionFailedException if the entity is at another version (412)
     */
    @Transactional
    public T update(ID id, @Nullable Long expectedVersion, Consumer<? super T> changes) {
        T entity = findById(id);
        if (expectedVersion != null && !expectedVersion.equals(versionOf(entity))) {
            throw new PreconditionFailedException("The resource has changed since it was read.");
        }
        changes.accept(entity);
        beforeUpdate(entity);
        return repository.save(entity);
    }

    @Transactional
    public void delete(ID id) {
        T entity = findById(id);
        beforeDelete(entity);
        repository.delete(entity);
    }

    /**
     * Optimistic locking version of the entity, used as its {@code ETag}. Defaults to {@link AuditableEntity#getVersion()};
     * {@code null} when the entity is not versioned.
     */
    public @Nullable Long versionOf(T entity) {
        return entity instanceof AuditableEntity auditable ? auditable.getVersion() : null;
    }

    /**
     * Runs before saving a new entity. Throw {@code BusinessException} to reject it.
     */
    protected void beforeCreate(T entity) {
    }

    /**
     * Runs after the changes are applied and before saving. Throw {@code BusinessException} to reject it.
     */
    protected void beforeUpdate(T entity) {
    }

    /**
     * Runs before deleting. Throw {@code BusinessException} to reject it.
     */
    protected void beforeDelete(T entity) {
    }
}
