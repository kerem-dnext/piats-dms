package com.piats.dms.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponseDTO {
    private UUID documentId;
    private String message;
    private String downloadUrl;
} 