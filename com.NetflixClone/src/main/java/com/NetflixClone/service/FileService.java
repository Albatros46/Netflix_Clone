package com.NetflixClone.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileService {
    String storeVideoFile(MultipartFile file);

    String storeImageFile(MultipartFile file);

    // Speichert Bytes unter einem bestimmten Namen (fuer die Beispieldaten)
    String storeImageBytes(byte[] content, String fileName);

    Path getVideoPath(String uuid);

    Resource loadImage(String uuid);

    void deleteVideo(String uuid);

    void deleteImage(String uuid);
}
