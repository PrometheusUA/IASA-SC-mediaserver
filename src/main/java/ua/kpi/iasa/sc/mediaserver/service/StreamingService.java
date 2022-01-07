package ua.kpi.iasa.sc.mediaserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StreamingService {
    private static final String FORMAT = "classpath:mp4/%s.mp4";
    private static final Path BASEPATH = Paths.get("./src/main/resources/mp4/");

    @Autowired
    private ResourceLoader resourceLoader;


    public Mono<Resource> getMP4Video(String name) {
        return Mono.fromSupplier(() -> this.resourceLoader.
                getResource( String.format(FORMAT, name)));
    }

    public Mono<ResponseEntity<Object>> saveMp4Video(FilePart file) {
        if(!file.filename().endsWith(".mp4"))
            return Mono.just(ResponseEntity.status(400).body("Not mp4 file"));
        Path dest = BASEPATH.resolve(file.filename());
        return file.transferTo(dest)
                .thenReturn(ResponseEntity.ok().build())
                .onErrorReturn(ResponseEntity.status(400).body("Error"));
    }
}
