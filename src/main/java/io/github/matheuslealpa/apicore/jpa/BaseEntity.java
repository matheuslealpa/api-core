package io.github.matheuslealpa.apicore.jpa;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.jspecify.annotations.Nullable;

import org.hibernate.proxy.HibernateProxy;

/**
 * Base entity with a generated UUID id and auditing. Equality by id, safe with Hibernate proxies.
 */
@MappedSuperclass
public abstract class BaseEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Nullable UUID id;

    protected BaseEntity() {
    }

    protected BaseEntity(BaseEntityBuilder<?, ?> builder) {
        super(builder);
    }

    public @Nullable UUID getId() {
        return id;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || effectiveClass(this) != effectiveClass(other)) {
            return false;
        }
        return id != null && Objects.equals(id, ((BaseEntity) other).getId());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    private static Class<?> effectiveClass(Object entity) {
        return entity instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : entity.getClass();
    }

    /**
     * Builder parent for Lombok's {@code @SuperBuilder} in subclasses. The id is generated, so it is not exposed.
     */
    public abstract static class BaseEntityBuilder<C extends BaseEntity, B extends BaseEntityBuilder<C, B>>
            extends AuditableEntityBuilder<C, B> {
    }
}
