package io.github.matheuslealpa.apicore.rsql;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;

import org.springframework.beans.factory.ObjectProvider;

/**
 * Default RSQL whitelist: the entity's basic attributes (including inherited ones), read from the JPA metamodel.
 * Associations, collections and embeddables stay blocked unless listed in {@code @RsqlFilter(allow = ...)}.
 */
class JpaBasicAttributes implements Function<Class<?>, List<String>> {

    private final ObjectProvider<EntityManagerFactory> entityManagerFactory;

    private final Map<Class<?>, List<String>> cache = new ConcurrentHashMap<>();

    JpaBasicAttributes(ObjectProvider<EntityManagerFactory> entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public List<String> apply(Class<?> entityType) {
        return cache.computeIfAbsent(entityType, this::basicAttributes);
    }

    private List<String> basicAttributes(Class<?> entityType) {
        EntityManagerFactory factory = entityManagerFactory.getIfAvailable();
        if (factory == null) {
            throw new IllegalStateException("RSQL filtering requires JPA: no EntityManagerFactory available");
        }
        try {
            return factory.getMetamodel().managedType(entityType).getAttributes().stream()
                    .filter(attribute -> attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC)
                    .map(Attribute::getName)
                    .sorted()
                    .toList();
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalStateException("RSQL filtering requires a JPA entity, but got " + entityType.getName(), ex);
        }
    }
}
