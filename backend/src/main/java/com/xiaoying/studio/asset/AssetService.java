package com.xiaoying.studio.asset;

import com.xiaoying.studio.common.ApiException;
import com.xiaoying.studio.identity.AppUser;
import com.xiaoying.studio.storage.StorageService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssetService {

    /** 服务端兜底的类型白名单，具体模板还能进一步收窄。 */
    private static final List<String> SUPPORTED = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final StorageService storageService;
    private final AssetMapper assetMapper;

    public AssetService(StorageService storageService, AssetMapper assetMapper) {
        this.storageService = storageService;
        this.assetMapper = assetMapper;
    }

    public AssetResponse upload(MultipartFile file, AppUser user) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "请选择要上传的图片");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("读取上传文件失败", e);
        }

        // 不信任 Content-Type：用魔数判断真实类型
        String detected = detectMimeType(bytes);
        if (detected == null || !SUPPORTED.contains(detected)) {
            throw ApiException.badRequest("UNSUPPORTED_TYPE", "只支持 JPG / PNG / WebP / GIF 图片");
        }

        StorageService.StoredFile stored = storageService.storeUpload(bytes, detected);
        Asset asset = new Asset(
                UUID.randomUUID().toString(),
                user.id(),
                sanitizeName(file.getOriginalFilename()),
                detected,
                bytes.length,
                stored.relativePath(),
                LocalDateTime.now());
        assetMapper.insert(asset);
        return toResponse(asset);
    }

    /**
     * 按传入顺序取素材，并校验归属：拿别人的 assetId 也拼不出任务。
     */
    public List<Asset> requireOwned(List<String> assetIds, AppUser user) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        List<Asset> found = assetMapper.findByIds(assetIds);
        Map<String, Asset> byId = new HashMap<>();
        for (Asset asset : found) {
            byId.put(asset.id(), asset);
        }

        List<Asset> ordered = new ArrayList<>(assetIds.size());
        for (String id : assetIds) {
            Asset asset = byId.get(id);
            if (asset == null) {
                throw ApiException.badRequest("ASSET_NOT_FOUND", "素材不存在：" + id);
            }
            if (!asset.userId().equals(user.id())) {
                throw ApiException.forbidden("不能使用其他账号的素材");
            }
            ordered.add(asset);
        }
        return ordered;
    }

    public AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.id(),
                asset.originalName(),
                asset.contentType(),
                asset.sizeBytes(),
                storageService.urlOf(asset.storagePath()));
    }

    private String sanitizeName(String original) {
        if (original == null || original.isBlank()) {
            return "upload";
        }
        String name = original.replaceAll("[\\r\\n\\\\/]", "_").trim();
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    /** 通过文件头判断真实图片类型，避免把 .jpg 后缀的任意文件当图片收下。 */
    static String detectMimeType(byte[] data) {
        if (data.length >= 3
                && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data.length >= 8
                && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
                && (data[4] & 0xFF) == 0x0D && (data[5] & 0xFF) == 0x0A
                && (data[6] & 0xFF) == 0x1A && (data[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        if (data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        if (data.length >= 4 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8') {
            return "image/gif";
        }
        return null;
    }

    public record AssetResponse(String id, String originalName, String contentType, long sizeBytes, String url) {
    }
}
