package ua.kpi.iasa.sc.mediaserver.api;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ua.kpi.iasa.sc.mediaserver.api.dto.ResourceBackDTO;
import ua.kpi.iasa.sc.mediaserver.security.utility.TokenUtility;
import ua.kpi.iasa.sc.mediaserver.service.StreamingService;
import ua.kpi.iasa.sc.resoursesgrpc.*;

import javax.json.JsonObject;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/video")
@CrossOrigin(origins = "http://localhost:3000")
public class StreamingController {
    private final StreamingService service;

    public StreamingController(StreamingService service){
        this.service = service;
    }

    @GetMapping(value = "/{name}", produces = "video/mp4")
    public Mono<Resource> getVideo(@PathVariable String name){
          return this.service.getMP4Video(name);
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> upload(@RequestPart FilePart file, @RequestHeader String authorization,
                                               @RequestPart String discipline,
                                               @RequestPart String teacher,
                                               @RequestPart String additionalInfo) {
        try {
            DecodedJWT decodedJWT = TokenUtility.verifyToken(authorization);
            Long createdById = decodedJWT.getClaim("id").asLong();
            List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            if(!roles.contains("Admin"))
                return Mono.just(ResponseEntity.status(403).body("You tried to upload video without permission"));
            Mono<ResponseEntity<Object>> resp = service.saveMp4Video(file);

            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8082)
                    .usePlaintext()
                    .build();

            ResourceGRPCServiceGrpc.ResourceGRPCServiceBlockingStub stub
                    = ResourceGRPCServiceGrpc.newBlockingStub(channel);

            final String plainFilename = file.filename().substring(0, file.filename().length() - ".mp4".length());

            ua.kpi.iasa.sc.resoursesgrpc.Resource.Builder builder = ua.kpi.iasa.sc.resoursesgrpc.Resource.newBuilder()
                    .setAdditionalInfo(additionalInfo)
                    .setDiscipline(discipline)
                    .setTeacher(teacher)
                    .setLink("http://localhost:8081/video/" + plainFilename)
                    .setAuthorId(createdById);

            CreatedResponse newResource = stub.createResource(builder.build());
            channel.shutdown();

            return resp;
        }
        catch (RuntimeException e){
            return Mono.just(ResponseEntity.status(400).body(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader String authorization){
        try {
            DecodedJWT decodedJWT = TokenUtility.verifyToken(authorization);
            List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            if (!roles.contains("Student") && !roles.contains("Teacher"))
                return ResponseEntity.status(403).body("You have no permission");

            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8082)
                    .usePlaintext()
                    .build();

            ResourceGRPCServiceGrpc.ResourceGRPCServiceBlockingStub stub
                    = ResourceGRPCServiceGrpc.newBlockingStub(channel);

            ResourceGRPCRequestMulti request = ResourceGRPCRequestMulti.getDefaultInstance();

            ResourceGRPCResponseMulti resources = stub.getAll(request);
            channel.shutdown();

            List<ResourceBackDTO> resourcesToSend = new ArrayList<>();


            for (ResourceBack r: resources.getResourcesList()){
                resourcesToSend.add(new ResourceBackDTO(r.getCreatedBy(), r.getId(),
                        new Timestamp(r.getCreatedAt().getSeconds() * 1000L), r.getAdditionalInfo(), r.getDiscipline(),
                        r.getLink(), r.getTeacher()));
            }

            return ResponseEntity.ok(resourcesToSend);
        }catch (Exception e){
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@RequestHeader String authorization, @PathVariable long id){
        try {
            DecodedJWT decodedJWT = TokenUtility.verifyToken(authorization);
            List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            if (!roles.contains("Student") && !roles.contains("Teacher"))
                return ResponseEntity.status(403).body("You have no permission");

            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8082)
                    .usePlaintext()
                    .build();

            ResourceGRPCServiceGrpc.ResourceGRPCServiceBlockingStub stub
                    = ResourceGRPCServiceGrpc.newBlockingStub(channel);

            ResourceGRPCRequest request = ResourceGRPCRequest.newBuilder()
                    .setId(id)
                    .build();

            ResourceBack resource = stub.getById(request);
            channel.shutdown();

            ResourceBackDTO resourceBack = new ResourceBackDTO(resource.getCreatedBy(), resource.getId(),
                    new Timestamp(resource.getCreatedAt().getSeconds() * 1000L), resource.getAdditionalInfo(), resource.getDiscipline(),
                    resource.getLink(), resource.getTeacher());

            return ResponseEntity.ok(resourceBack);
        }catch (Exception e){
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}
