package com.eximee.los.controller;

import com.eximee.los.domain.DocumentType;
import com.eximee.los.domain.User;
import com.eximee.los.dto.DocumentDto;
import com.eximee.los.service.AuthService;
import com.eximee.los.service.DocumentStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Documents", description = "Supporting document upload and download endpoints")
public class DocumentController {

    private final DocumentStorageService documentService;
    private final AuthService authService;

    public DocumentController(DocumentStorageService documentService, AuthService authService) {
        this.documentService = documentService;
        this.authService = authService;
    }

    @PostMapping(value = "/applications/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload supporting document for an application")
    public ResponseEntity<DocumentDto> uploadDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file
    ) {
        User user = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(documentService.storeDocument(id, user, documentType, file));
    }

    @GetMapping("/applications/{id}/documents")
    @Operation(summary = "Get list of all documents uploaded for an application")
    public ResponseEntity<List<DocumentDto>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentsByApplication(id));
    }

    @GetMapping("/documents/{docId}/download")
    @Operation(summary = "Download or preview an uploaded document")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long docId
    ) {
        User user = authService.getUserByEmail(userDetails.getUsername());
        Resource resource = documentService.loadAsResource(docId, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
