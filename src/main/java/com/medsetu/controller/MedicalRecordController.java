package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.MedicalRecord;
import com.medsetu.entity.User;
import com.medsetu.service.MedicalRecordService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class MedicalRecordController {

    private final MedicalRecordService recordService;

    public MedicalRecordController(MedicalRecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @AuthenticationPrincipal User currentUser,
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String recordDate) {
        Map<String, Object> record = recordService.uploadRecord(
                currentUser.getId(), file, title, fileType, recordDate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Record uploaded.", record));
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyRecords(
            @AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> records = recordService.getPatientRecords(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Records retrieved.", records));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        MedicalRecord record = recordService.getRecordById(id, currentUser.getId());
        try {
            Resource resource = new UrlResource(Paths.get(record.getFileUrl()).toAbsolutePath().toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getTitle() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        recordService.deleteRecord(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Record deleted."));
    }
}
