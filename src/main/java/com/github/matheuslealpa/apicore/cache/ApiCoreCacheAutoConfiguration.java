package com.github.matheuslealpa.apicore.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.matheuslealpa.apicore.config.ApiCoreProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * Habilita {@code @Cacheable} com Caffeine. Estatísticas ficam ligadas para expor métricas no Actuator.
 */
@AutoConfiguration(before = CacheAutoConfiguration.class)
@ConditionalOnClass({Caffeine.class, CaffeineCacheManager.class})
@ConditionalOnBooleanProperty(name = "api-core.cache.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ApiCoreProperties.class)
@EnableCaching
public class ApiCoreCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CaffeineCacheManager cacheManager(ApiCoreProperties properties) {
        ApiCoreProperties.Cache cache = properties.cache();
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeineSpec(withStats(cache.defaultSpec()));
        cache.specs().forEach((name, spec) ->
                manager.registerCustomCache(name, Caffeine.from(withStats(spec)).build()));
        return manager;
    }

    private static CaffeineSpec withStats(String spec) {
        if (spec == null || spec.isBlank()) {
            return CaffeineSpec.parse("recordStats");
        }
        return CaffeineSpec.parse(spec.contains("recordStats") ? spec : spec + ",recordStats");
    }
}
