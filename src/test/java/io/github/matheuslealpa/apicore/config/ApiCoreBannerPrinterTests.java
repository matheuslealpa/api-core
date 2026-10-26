package io.github.matheuslealpa.apicore.config;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCoreBannerPrinterTests {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Test
    void imprimeBannerPorPadrao() {
        print(Map.of());

        assertThat(output()).contains("(_)", " :: api-core :: ", "(vdev)");
    }

    @Test
    void respeitaBannerModeOff() {
        print(Map.of("spring.main.banner-mode", "off"));

        assertThat(output()).isEmpty();
    }

    @Test
    void podeSerDesligado() {
        print(Map.of("api-core.banner.enabled", "false"));

        assertThat(output()).isEmpty();
    }

    private void print(Map<String, Object> properties) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        new ApiCoreBannerPrinter(new PrintStream(output, true, StandardCharsets.UTF_8))
                .onApplicationEvent(new ApplicationContextInitializedEvent(new SpringApplication(), new String[0], context));
    }

    private String output() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
