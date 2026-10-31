package com.NetflixClone.serviceImpl;

import com.NetflixClone.exception.ResourceNotFoundException;
import com.NetflixClone.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "ogg", "mov", "m4v", "mkv");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");

    private final Path videoDir;
    private final Path imageDir;

    public FileServiceImpl(@Value("${file.upload.video-dir:upload/videos}") String videoDir,
                           @Value("${file.upload.image-dir:upload/images}") String imageDir) throws IOException {
        this.videoDir = Paths.get(videoDir).toAbsolutePath().normalize();
        this.imageDir = Paths.get(imageDir).toAbsolutePath().normalize();
        Files.createDirectories(this.videoDir);
        Files.createDirectories(this.imageDir);
    }

    @Override
    public String storeVideoFile(MultipartFile file) {
        return store(file, videoDir, VIDEO_EXTENSIONS, "video");
    }

    @Override
    public String storeImageFile(MultipartFile file) {
        return store(file, imageDir, IMAGE_EXTENSIONS, "image");
    }

    @Override
    public String storeImageBytes(byte[] content, String fileName) {
        try {
            Files.write(resolve(imageDir, fileName), content);
            return fileName;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store image " + fileName, e);
        }
    }

    @Override
    public Path getVideoPath(String uuid) {
        Path path = resolve(videoDir, uuid);
        if (!Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Video file not found: " + uuid);
        }
        return path;
    }

    @Override
    public Resource loadImage(String uuid) {
        Path path = resolve(imageDir, uuid);
        if (!Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Image file not found: " + uuid);
        }
        return new FileSystemResource(path);
    }

    @Override
    public void deleteVideo(String uuid) {
        delete(videoDir, uuid);
    }

    @Override
    public void deleteImage(String uuid) {
        delete(imageDir, uuid);
    }

    private String store(MultipartFile file, Path dir, Set<String> allowed, String kind) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a " + kind + " file!");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!allowed.contains(extension)) {
            throw new IllegalArgumentException("Unsupported " + kind + " format: ." + extension);
        }
        String name = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store " + kind + " file", e);
        }
        return name;
    }

    private void delete(Path dir, String uuid) {
        if (uuid == null || uuid.isBlank()) return;
        try {
            Files.deleteIfExists(resolve(dir, uuid));
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Could not delete file {}: {}", uuid, e.getMessage());
        }
    }

    // Verhindert Path-Traversal (../) ueber den Dateinamen
    private static Path resolve(Path dir, String name) {
        Path path = dir.resolve(name).normalize();
        if (!path.getParent().equals(dir)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return path;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
