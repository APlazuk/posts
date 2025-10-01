package pl.aplazuk.postbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.aplazuk.postbackend.model.Post;
import pl.aplazuk.postbackend.service.PostService;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<List<Post>> getPosts() {
        List<Post> allPosts = postService.getAllPosts();
        return !allPosts.isEmpty() ? ResponseEntity.ok(allPosts) : ResponseEntity.notFound().build();
    }
}
