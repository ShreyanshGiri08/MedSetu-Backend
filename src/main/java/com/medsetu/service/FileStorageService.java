package com.medsetu.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for file storage — enables swapping local storage for AWS S3 later.
 */
public interface FileStorageService {
    String store(MultipartFile file);
    void delete(String path);
    String getDownloadUrl(String path);
}
