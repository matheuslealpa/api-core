package io.github.matheuslealpa.apicore.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCoreCacheAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ApiCoreCacheAutoConfiguration.class, CacheAutoConfiguration.class));

    @Test
    void registraCaffeineComSpecsPorCache() {
        runner.withPropertyValues("api-core.cache.specs.produtos=maximumSize=10,expireAfterWrite=1m")
                .run(context -> {
                    CaffeineCacheManager manager = context.getBean(CaffeineCacheManager.class);
                    assertThat(manager.getCacheNames()).contains("produtos");
                    Cache<Object, Object> produtos = ((CaffeineCache) manager.getCache("produtos")).getNativeCache();
                    assertThat(produtos.policy().eviction()).hasValueSatisfying(
                            eviction -> assertThat(eviction.getMaximum()).isEqualTo(10));
                    assertThat(produtos.policy().isRecordingStats()).isTrue();
                    Cache<Object, Object> outro = ((CaffeineCache) manager.getCache("outro")).getNativeCache();
                    assertThat(outro.policy().eviction()).hasValueSatisfying(
                            eviction -> assertThat(eviction.getMaximum()).isEqualTo(1000));
                });
    }

    @Test
    void recuaQuandoAplicacaoDefineCacheManager() {
        runner.withUserConfiguration(CustomCacheManager.class)
                .run(context -> assertThat(context.getBean(CacheManager.class))
                        .isInstanceOf(ConcurrentMapCacheManager.class));
    }

    @Test
    void podeSerDesligado() {
        runner.withPropertyValues("api-core.cache.enabled=false", "spring.cache.type=none")
                .run(context -> assertThat(context).doesNotHaveBean(CaffeineCacheManager.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomCacheManager {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
