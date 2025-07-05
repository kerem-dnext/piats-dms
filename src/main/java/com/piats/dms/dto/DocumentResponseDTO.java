package com.piats.dms.dto;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DocumentResponseDTO {
    private UUID id;
    private UUID applicationId;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private Instant createdAt;
    private Instant updatedAt;
} 