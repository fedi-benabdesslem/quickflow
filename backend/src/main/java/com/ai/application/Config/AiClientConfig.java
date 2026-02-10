package com.ai.application.Config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

/**
 * Configures the HTTP client used by Spring AI for LLM API calls.
 * 
 * The default OkHttp timeout (~30s) is too short for large prompts
 * like Voice Mode transcripts. This configuration increases timeouts
 * to handle longer Gemini API processing times.
 */
@Configuration
public class AiClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build();

        return RestClient.builder()
                .requestFactory(new OkHttp3ClientHttpRequestFactory(client));
    }
}
