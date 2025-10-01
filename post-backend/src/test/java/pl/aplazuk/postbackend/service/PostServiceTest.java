package pl.aplazuk.postbackend.service;

import brave.Tracer;
import brave.Tracing;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import pl.aplazuk.postbackend.exception.JsonFileProcessingException;
import pl.aplazuk.postbackend.exception.NoPostFoundException;
import pl.aplazuk.postbackend.model.Post;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ObjectMapper objectMapper;

    private PostService postService;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @TempDir
    private Path tempDir;

    private List<Post> mockedPosts;

    @Captor
    ArgumentCaptor<File> fileCaptor;

    @Captor
    ArgumentCaptor<Post> postCaptor;


    @BeforeEach()
    void setUp() {
        Tracing braveTracing = Tracing.newBuilder().build();
        Tracer tracer = braveTracing.tracer();

        mockedPosts = List.of(
                new Post(1, 1, "t1", "b1"),
                new Post(2, 2, "qui est esse", "b2"),
                new Post(3, 3, "jak kosić trawę i nie zwariować", "praktyczny poradnik")
        );

        String postDataDir = tempDir.toString();
        postService = new PostService(postDataDir, restClient, objectMapper, tracer);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/posts"))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);

        fileCaptor = ArgumentCaptor.forClass(File.class);
        postCaptor = ArgumentCaptor.forClass(Post.class);
    }

    @Test
    void shouldReturnAllPosts() throws IOException {
        //given
        when(responseSpec.body(new ParameterizedTypeReference<List<Post>>() {
        })).thenReturn(mockedPosts);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        //when
        List<Post> actual = postService.getAllPosts();

        //then
        assertNotNull(actual);
        assertEquals(mockedPosts.size(), actual.size());
        assertEquals("jak kosić trawę i nie zwariować", actual.get(2).getTitle());

        verify(objectMapper, times(1)).writeValue(fileCaptor.capture(), eq(actual.get(0)));
        verify(objectMapper, times(1)).writeValue(fileCaptor.capture(), eq(actual.get(1)));
        verify(objectMapper, times(1)).writeValue(fileCaptor.capture(), eq(actual.get(2)));

        List<File> allValues = fileCaptor.getAllValues();
        assertEquals(allValues.size(), actual.size());
        assertTrue(allValues.get(0).getPath().endsWith("1.json"));
        assertTrue(allValues.get(1).getPath().endsWith("2.json"));
        assertTrue(allValues.get(2).getPath().endsWith("3.json"));
    }

    @Test
    void shouldNotReturnPostsListWhenNoPostsFound() throws IOException {
        //given
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(new ParameterizedTypeReference<List<Post>>() {
        }))
                .thenThrow(new NoPostFoundException("Posts has not been found", HttpStatus.NOT_FOUND.getReasonPhrase()));

        //when
        NoPostFoundException actual = assertThrows(NoPostFoundException.class, () -> postService.getAllPosts());

        //then
        assertNotNull(actual);
        assertTrue(actual.getMessage().contains("Posts has not been found |Cause:"));

        verify(objectMapper, never()).writeValue(any(File.class), postCaptor.capture());
        List<Post> allPosts = postCaptor.getAllValues();
        assertTrue(allPosts.isEmpty());
    }

    @Test
    void shouldNotSaveFilesWhenPostsAreEmpty() throws IOException {
        //given
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(new ParameterizedTypeReference<List<Post>>() {
        })).thenReturn(Collections.emptyList());

        //when
        List<Post> actual = postService.getAllPosts();

        //then
        assertTrue(actual.isEmpty());
        verify(objectMapper, never()).writeValue(any(File.class), any(Post.class));

    }

    @Test
    void shouldThrowJsonFileProcessingExceptionWhenIOExceptionOccurs() throws IOException {
        //given
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(new ParameterizedTypeReference<List<Post>>() {
        })).thenReturn(mockedPosts);

        doThrow(new IOException("I/O Error"))
                .when(objectMapper).writeValue(any(File.class), any(Post.class));

        //when
        JsonFileProcessingException actual = assertThrows(JsonFileProcessingException.class, () -> postService.getAllPosts());

        //then
        assertNotNull(actual);
        assertTrue(actual.getMessage().contains("Could not save posts to file!"));
    }

}