package com.xiaoying.studio.template;

import java.time.LocalDateTime;

/**
 * 模板。{@code acceptedMimeTypes} 在库里存逗号分隔字符串，出参时拆成数组。
 */
public record Template(
        String id,
        String title,
        String category,
        String coverUrl,
        String description,
        int requiredImageCount,
        String acceptedMimeTypes,
        long maxFileSizeBytes,
        int sortOrder,
        LocalDateTime createdAt) {
}
