package io.github.matheuslealpa.apicore.rsql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Converts an RSQL expression from the request into a {@code Specification<T>}.
 *
 * <pre>{@code
 * @GetMapping
 * Page<Product> list(@RsqlFilter(allow = {"name", "price"}) Specification<Product> filter, Pageable pageable) {
 *     return repository.findAll(filter, pageable);
 * }
 * // GET /products?filter=name=like=keyb*;price=gt=100
 * }</pre>
 *
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
     * Entity properties that can be filtered. Empty allows all of them; prefer an explicit list in public
     * APIs to avoid exposing internal fields or allowing arbitrary joins.
     */
    String[] allow() default {};

    /**
     * Applies {@code distinct} to the query, useful when filtering by collections.
     */
    boolean distinct() default false;
}
