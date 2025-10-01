package pl.aplazuk.postbackend.config;

import brave.Span;
import brave.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class Config {

    private static final String POST_URL = "https://jsonplaceholder.typicode.com";

    private final Tracer tracer;

    public Config(Tracer tracer) {
        this.tracer = tracer;
    }

    @Bean
    public RestClient getRestClient() {
        return RestClient.builder()
                .baseUrl(POST_URL)
                .requestInterceptor((request, body, execution) -> {
                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan != null) {
                        request.getHeaders().add("X-B3-TraceId", currentSpan.context().traceIdString());
                        request.getHeaders().add("X-B3-SpanId", currentSpan.context().spanIdString());
                        request.getHeaders().add("X-B3-Sampled", "1");
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
