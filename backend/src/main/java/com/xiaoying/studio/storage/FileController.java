package com.xiaoying.studio.storage;

import com.xiaoying.studio.common.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一下载入口：{@code /api/files/**}。
 * <ul>
 *   <li>{@code covers/*.svg}：随包内置的示例封面（classpath）</li>
 *   <li>{@code uploads/**}：用户上传素材</li>
 *   <li>{@code results/**}：制作结果</li>
 * </ul>
 */
@RestController
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/api/files/{*path}")
    public ResponseEntity<Resource> download(@PathVariable String path) {
        String relative = path.startsWith("/") ? path.substring(1) : path;

        if (relative.startsWith("covers/")) {
            String filename = relative.substring("covers/".length());
            InputStream stream = storageService.openBundledCover(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/svg+xml"))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(new InputStreamResource(stream));
        }

        Path file = storageService.resolve(relative);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw ApiException.notFound("文件不存在：" + relative);
        }
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                contentType = MediaType.parseMediaType(probed);
            }
        } catch (IOException ignored) {
            // 探测失败就退回 octet-stream，不影响下载
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(new FileSystemResource(file));
    }
}
