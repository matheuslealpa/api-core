package io.github.matheuslealpa.apicore.rsql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Converte uma expressão RSQL da requisição em {@code Specification<T>}.
 *
 * <pre>{@code
 * @GetMapping
 * Page<Produto> listar(@RsqlFilter(allow = {"nome", "preco"}) Specification<Produto> filtro, Pageable pageable) {
 *     return repository.findAll(filtro, pageable);
 * }
 * // GET /produtos?filter=nome=like=tecl*;preco=gt=100
 * }</pre>
 *
 * Sem o parâmetro na requisição, a specification não restringe nada.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RsqlFilter {

    /**
     * Nome do parâmetro da requisição. Vazio usa {@code api-core.rsql.param-name} (padrão {@code filter}).
     */
    String param() default "";

    /**
     * Propriedades da entidade que podem ser filtradas. Vazio libera todas; prefira listar explicitamente
     * em APIs públicas para não expor campos internos nem permitir joins arbitrários.
     */
    String[] allow() default {};

    /**
     * Aplica {@code distinct} na consulta, útil ao filtrar por coleções.
     */
    boolean distinct() default false;
}
