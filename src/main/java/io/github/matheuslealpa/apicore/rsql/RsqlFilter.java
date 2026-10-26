package io.github.matheuslealpa.apicore.rsql;

import java.lang.annotation.*;

/**
 * Customizes the RSQL filter of a {@code Specification<T>} controller parameter. Optional: every
 * {@code Specification<T>} parameter is resolved from the request, and without {@link #allow()} only the
 * entity's basic attributes can be filtered.
 *
 * <pre>{@code
 * @GetMapping
 * Page<Product> list(@RsqlFilter(allow = {"name", "price"}) Specification<Product> filter, Pageable pageable) {
 *     return repository.findAll(filter, pageable);
 * }
 * // GET /products?filter=name=like=keyb*;price=gt=100
 * }</pre>
 * <p>
 * Without the request parameter, the specification does not restrict anything.
 *
 * <p>On a controller class, it configures the {@code @RsqlFilter} parameters declared without attributes,
 * such as the list endpoint inherited from {@code CrudController}.
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RsqlFilter {

    /**
     * Request parameter name. Empty uses {@code api-core.rsql.param-name} (default {@code filter}).
     */
    String param() default "";

    /**
     * Entity properties that can be filtered. Empty allows only the entity's basic attributes (no associations,
     * collections or embeddables); {@code "*"} allows everything, including joins.
     */
    String[] allow() default {};

    /**
     * Applies {@code distinct} to the query, useful when filtering by collections.
     */
    boolean distinct() default false;
}
