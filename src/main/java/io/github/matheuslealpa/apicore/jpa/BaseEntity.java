package io.github.matheuslealpa.apicore.jpa;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.proxy.HibernateProxy;

/**
 * Entidade base com id UUID gerado e auditoria. Igualdade por id, segura com proxies do Hibernate.
 */
@MappedSuperclass
public abstract class BaseEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public UUID getId() {
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
}
