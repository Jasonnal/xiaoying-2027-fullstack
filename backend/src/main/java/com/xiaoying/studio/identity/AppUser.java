package com.xiaoying.studio.identity;

import java.time.LocalDateTime;

/**
 * 用户。演示型身份：没有口令，只有 handle，登录即创建/复用账号。
 */
public record AppUser(
        String id,
        String handle,
        String displayName,
        String accentColor,
        LocalDateTime createdAt) {
}
