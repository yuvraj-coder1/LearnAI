package com.example.LearnAI.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@ConditionalOnMissingBean(R2StorageService.class)
public class LocalStorageService implements StorageService {

    private final Path storagePath;

    public LocalStorageService(@Value("${storage.local.path:./uploads}") String path) throws IOException {
        this.storagePath = Path.of(path);
        Files.createDirectories(storagePath);
    }

    @Override
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        try {
            Path filePath = storagePath.resolve(key);
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return Files.newInputStream(storagePath.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(storagePath.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}
