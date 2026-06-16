package com.stormapi.engine.http;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientFactoryTest {

    @Test
    void create_returnsConfiguredClient() {
        HttpClient client = HttpClientFactory.create(Duration.ofSeconds(3));

        assertThat(client).isNotNull();
        assertThat(client.connectTimeout()).isPresent();
        assertThat(client.connectTimeout().get()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void create_usesHttp11() {
        HttpClient client = HttpClientFactory.create(Duration.ofSeconds(5));

        assertThat(client.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
    }

    @Test
    void create_followsNoRedirects() {
        HttpClient client = HttpClientFactory.create(Duration.ofSeconds(5));

        assertThat(client.followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
    }

    @Test
    void createDefault_uses5SecondTimeout() {
        HttpClient client = HttpClientFactory.createDefault();

        assertThat(client.connectTimeout()).isPresent();
        assertThat(client.connectTimeout().get()).isEqualTo(Duration.ofSeconds(5));
    }

}
