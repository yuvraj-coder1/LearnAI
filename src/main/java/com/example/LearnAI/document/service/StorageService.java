package com.example.LearnAI.document.service;

import java.io.InputStream;

public interface StorageService {

    String upload(String key, InputStream inputStream, long contentLength, String contentType);

    InputStream download(String key);

    void delete(String key);
}
