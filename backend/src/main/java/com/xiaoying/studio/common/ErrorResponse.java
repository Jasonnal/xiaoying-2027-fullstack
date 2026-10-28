package com.xiaoying.studio.common;

import java.time.LocalDateTime;

/**
 * 统一错误体。前端只依赖 code 做分支，message 直接展示给用户。
 */
public record ErrorResponse(String code, String message, LocalDateTime timestamp) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, LocalDateTime.now());
    }
}
