package ua.kpi.iasa.sc.mediaserver.api;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ua.kpi.iasa.sc.mediaserver.security.utility.TokenUtility;
import ua.kpi.iasa.sc.mediaserver.service.StreamingService;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/video")
@CrossOrigin(origins = "http://localhost:3000")
public class StreamingController {
    private StreamingService service;

    public StreamingController(StreamingService service){
        this.service = service;
    }

    @GetMapping(value = "/{name}", produces = "video/mp4")
    public Mono<Resource> getVideo(@PathVariable String name){
//        try {
          return this.service.getMP4Video(name);
//        }
//        catch (Exception e){
//            return ServerResponse.status(400).build();
//        }
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> upload(@RequestPart FilePart file, @RequestHeader String authorization) {
        try {
//            if(!authorization.startsWith("Bearer ")){
//                return Mono.just("Bearer token isn't in appropriate format!");
//            }
            DecodedJWT decodedJWT = TokenUtility.verifyToken(authorization);
            //        String email = decodedJWT.getSubject();
            List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            if(!roles.contains("Admin"))
                return Mono.just(ResponseEntity.status(403).body("You tried to upload video without permission"));
            return service.saveMp4Video(file);
        }
        catch (RuntimeException e){
            return Mono.just(ResponseEntity.status(400).body(e.getMessage()));
        }
    }
}
