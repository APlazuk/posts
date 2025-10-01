package pl.aplazuk.postbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pl.aplazuk.postbackend.exception.JsonFileProcessingException;
import pl.aplazuk.postbackend.exception.NoPostFoundException;
import pl.aplazuk.postbackend.exception.PostServiceTemporaryUnavailable;
import pl.aplazuk.postbackend.model.Post;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostService.class);

    private final String postDataDir;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PostService(@Value("${post.data-dir:}") String postDataDir, RestClient restClient, ObjectMapper objectMapper) {
        this.postDataDir = postDataDir;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @CircuitBreaker(name = "callPostApi", fallbackMethod = "fallbackGetAllPosts")
    public List<Post> getAllPosts() {
           Optional<List<Post>> fetchedPosts = Optional.ofNullable(restClient.get()
                   .uri("/posts")
                   .retrieve()
                   .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                       if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                           throw new NoPostFoundException("Posts has not been found", response.getStatusText());
                       }
                       throw new HttpClientErrorException(response.getStatusCode(), response.getStatusText());
                   })
                   .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                       throw new PostServiceTemporaryUnavailable(response.getStatusCode(), response.getStatusText());
                   })
                   .body(new ParameterizedTypeReference<List<Post>>() {
                   }));
           fetchedPosts.ifPresent(this::savePostToFile);
           return fetchedPosts.orElse(Collections.emptyList());
    }

    public List<Post> fallbackGetAllPosts(Throwable throwable) {
        LOGGER.warn("Call to external api has failed. Cause: {}", throwable.getMessage());

        Path path = Path.of(postDataDir + "101.json");
        if (Files.notExists(path) || !Files.isRegularFile(path)) {
            LOGGER.warn("File with stored data does not exist");
            throw new NoPostFoundException("Posts has not been found");
        }

        try (InputStream fin = Files.newInputStream(path)) {
            Post post = objectMapper.readValue(fin, Post.class);
            return List.of(post);
        } catch (IOException e) {
            LOGGER.error("Failed reading stored post. Cause: {}", e.getMessage());
            throw new PostServiceTemporaryUnavailable("Sorry 😔");
        }
    }

    public void savePostToFile(List<Post> fetchedPosts) {
        try {
            if (!fetchedPosts.isEmpty()) {
                createNewDirectoryIfNotExists();
                for (Post fetchedPost : fetchedPosts) {
                    objectMapper.writeValue(new File(postDataDir + fetchedPost.getId() + ".json"), fetchedPost);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while saving posts to file!", e);
            throw new JsonFileProcessingException("Could not save posts to file!", e);
        }
    }

    private void createNewDirectoryIfNotExists() throws IOException {
        Path path = Path.of(postDataDir);
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        LOGGER.info("Directory {} created", postDataDir);
    }

    //napisać testy jednostkowe -->done
    //spróbować sobie z profilami--> warto to dodać ze względu na bazę danych --> na tym poziomie nie ma sensu, ale trzeba pamiętać o active profiles na testach integracyjnych jeśli będzie dodana baza danych, tak aby nie naruszyc bazy prod
    //napisać sobie cricuit breaker i ewentualnie redis --> tutaj prostą metodę na fallback wczytującą ostatni plik
    //dodać monitoring zipkin
    //dodac warstwę security
    //napisać sobie metodę która pobiera te pliki za pomoca calla w kontrolerze
    //napisać sobie metodę która zapisuje te dane do bazy danych --> zastanowić się jaka
}
