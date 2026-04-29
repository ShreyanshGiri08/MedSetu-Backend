package com.medsetu.service;

import com.medsetu.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final FileUtil fileUtil;

    public LocalFileStorageService(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    @Override
    public String store(MultipartFile file) {
        try {
            return fileUtil.saveFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        fileUtil.deleteFile(path);
    }

    @Override
    public String getDownloadUrl(String path) {
        return "/" + path;
    }
}
