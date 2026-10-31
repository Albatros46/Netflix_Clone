package com.NetflixClone.controller;

import com.NetflixClone.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
public class FileController {

    // Groesse eines Streaming-Abschnitts bei Range-Anfragen (Spulen im Player)
    private static final long CHUNK_SIZE = 1024 * 1024;

    @Autowired
    private FileService fileService;

    @PostMapping("/upload/video")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(Map.of("uuid", fileService.storeVideoFile(file)));
    }

    @PostMapping("/upload/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(Map.of("uuid", fileService.storeImageFile(file)));
    }

    // Video-Streaming mit HTTP-Range-Unterstuetzung (206 Partial Content)
    @GetMapping("/video/{uuid}")
    public ResponseEntity<ResourceRegion> streamVideo(@PathVariable String uuid,
                                                      @RequestHeader HttpHeaders headers) throws IOException {
        Path path = fileService.getVideoPath(uuid);
        Resource video = new FileSystemResource(path);
        long length = video.contentLength();
        MediaType mediaType = MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM);

        List<HttpRange> ranges = headers.getRange();
        ResourceRegion region;
        if (ranges.isEmpty()) {
            region = new ResourceRegion(video, 0, Math.min(CHUNK_SIZE, length));
        } else {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(length);
            long end = range.getRangeEnd(length);
            region = new ResourceRegion(video, start, Math.min(CHUNK_SIZE, end - start + 1));
        }
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }

    @GetMapping("/image/{uuid}")
    public ResponseEntity<Resource> getImage(@PathVariable String uuid) {
        Resource image = fileService.loadImage(uuid);
        MediaType mediaType = MediaTypeFactory.getMediaType(image).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(image);
    }
}
