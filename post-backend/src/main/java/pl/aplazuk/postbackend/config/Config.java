package pl.aplazuk.postbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class Config {

    private static final String POST_URL = "https://jsonplaceholder.typicode.com";

    @Bean
    public RestClient getRestClient() {
        return RestClient.builder().baseUrl(POST_URL).build();
    }
}
