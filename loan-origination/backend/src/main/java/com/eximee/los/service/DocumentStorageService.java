package com.eximee.los.service;

import com.eximee.los.domain.*;
import com.eximee.los.dto.DocumentDto;
import com.eximee.los.repository.ApplicationDocumentRepository;
import com.eximee.los.repository.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path uploadLocation;
    private final ApplicationDocumentRepository documentRepository;
    private final LoanApplicationRepository applicationRepository;

    public DocumentStorageService(
            @Value("${app.upload.dir:./uploads}") String uploadDir,
            ApplicationDocumentRepository documentRepository,
            LoanApplicationRepository applicationRepository
    ) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.documentRepository = documentRepository;
        this.applicationRepository = applicationRepository;
        try {
            Files.createDirectories(this.uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Transactional
    public DocumentDto storeDocument(Long applicationId, User currentUser, DocumentType documentType, MultipartFile file) {
        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        // Check permission
        boolean isApplicant = application.getApplicant().getId().equals(currentUser.getId());
        boolean isStaff = currentUser.getRole() == Role.ROLE_OFFICER || currentUser.getRole() == Role.ROLE_ADMIN;
        if (!isApplicant && !isStaff) {
            throw new SecurityException("Not authorized to upload documents for this application");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            fileExtension = originalFilename.substring(dotIdx);
        }
        String storedFilename = UUID.randomUUID() + fileExtension;

        try {
            Path targetDir = this.uploadLocation.resolve(String.valueOf(applicationId));
            Files.createDirectories(targetDir);
            Path targetLocation = targetDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            ApplicationDocument doc = new ApplicationDocument();
            doc.setApplication(application);
            doc.setDocumentType(documentType);
            doc.setFileName(originalFilename);
            doc.setFileSize(file.getSize());
            doc.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            doc.setStoragePath(targetLocation.toString());

            ApplicationDocument saved = documentRepository.save(doc);

            return new DocumentDto(
                    saved.getId(),
                    saved.getDocumentType(),
                    saved.getFileName(),
                    saved.getFileSize(),
                    saved.getContentType(),
                    saved.getUploadedAt()
            );
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + originalFilename, ex);
        }
    }

    public Resource loadAsResource(Long docId, User currentUser) {
        ApplicationDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        boolean isApplicant = doc.getApplication().getApplicant().getId().equals(currentUser.getId());
        boolean isStaff = currentUser.getRole() == Role.ROLE_OFFICER || currentUser.getRole() == Role.ROLE_ADMIN;
        if (!isApplicant && !isStaff) {
            throw new SecurityException("Not authorized to access this document");
        }

        try {
            Path filePath = Paths.get(doc.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + doc.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + doc.getFileName(), e);
        }
    }

    public List<DocumentDto> getDocumentsByApplication(Long applicationId) {
        return documentRepository.findByApplicationId(applicationId).stream()
                .map(d -> new DocumentDto(
                        d.getId(),
                        d.getDocumentType(),
                        d.getFileName(),
                        d.getFileSize(),
                        d.getContentType(),
                        d.getUploadedAt()
                ))
                .toList();
    }
}
