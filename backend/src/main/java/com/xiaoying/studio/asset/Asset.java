package com.xiaoying.studio.asset;

import java.time.LocalDateTime;

/**
 * 用户上传的素材。{@code storagePath} 是相对存储根目录的路径，出参时拼成 {@code /api/files/...}。
 */
public record Asset(
        String id,
        String userId,
        String originalName,
        String contentType,
        long sizeBytes,
        String storagePath,
        LocalDateTime createdAt) {
}
