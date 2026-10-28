package com.xiaoying.studio.identity;

import java.time.LocalDateTime;

/**
 * 用户。{@code passwordHash} 只参与服务端校验，任何接口出参都不会序列化它。
 */
public record AppUser(
        String id,
        String handle,
        String displayName,
        String passwordHash,
        String accentColor,
        LocalDateTime createdAt) {
}
