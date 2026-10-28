package com.xiaoying.studio.storage;

import com.xiaoying.studio.common.ApiException;
import com.xiaoying.studio.config.AppProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 本地文件存储。用户素材与结果都落在 {@code app.storage.root} 下，返回给前端的是
 * 相对 URL（{@code /api/files/...}），由 {@link FileController} 统一提供读取。
 *
 * <p>没有做对象存储：题目明确允许本地目录/容器数据卷。
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;
    private final String publicBasePath;

    public StorageService(AppProperties properties) {
        this.root = Paths.get(properties.storage().root()).toAbsolutePath().normalize();
        this.publicBasePath = properties.storage().publicBasePath();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root.resolve("uploads"));
            Files.createDirectories(root.resolve("results"));
            log.info("存储根目录：{}", root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建存储目录：" + root, e);
        }
    }

    public Path root() {
        return root;
    }

    /**
     * 保存上传的图片，返回相对路径（如 {@code uploads/2026/10/uuid.png}）。
     * 用 UUID 命名，避免中文/特殊字符进入文件系统路径。
     */
    public StoredFile storeUpload(byte[] content, String contentType) {
        String ext = extensionOf(contentType);
        String relative = "uploads/" + LocalDate.now().format(MONTH) + "/" + UUID.randomUUID() + "." + ext;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new IllegalStateException("写入素材失败", e);
        }
        return new StoredFile(relative, contentType, content.length);
    }

    public Path resultPath(String jobId) {
        Path path = resolve("results/" + jobId + ".jpg");
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("无法创建结果目录", e);
        }
        return path;
    }

    public String resultUrl(String jobId) {
        return publicBasePath + "/results/" + jobId + ".jpg";
    }

    /** 解析相对路径，越界直接拒绝（防目录穿越）。 */
    public Path resolve(String relativePath) {
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw ApiException.badRequest("INVALID_PATH", "非法的文件路径");
        }
        return candidate;
    }

    public String urlOf(String relativePath) {
        return publicBasePath + "/" + relativePath;
    }

    /** 内置示例封面放在 jar 内 classpath:demo/covers，随包发布，不依赖外部图床。 */
    public InputStream openBundledCover(String filename) {
        if (!filename.matches("[a-z0-9-]+\\.svg")) {
            throw ApiException.notFound("封面不存在");
        }
        ClassPathResource resource = new ClassPathResource("demo/covers/" + filename);
        if (!resource.exists()) {
            throw ApiException.notFound("封面不存在");
        }
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("读取封面失败", e);
        }
    }

    private String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> throw ApiException.badRequest("UNSUPPORTED_TYPE", "不支持的图片类型：" + contentType);
        };
    }

    public record StoredFile(String relativePath, String contentType, long sizeBytes) {
    }
}
