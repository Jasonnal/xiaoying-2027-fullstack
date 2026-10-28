package com.xiaoying.studio.asset;

import com.xiaoying.studio.asset.AssetService.AssetResponse;
import com.xiaoying.studio.identity.AppUser;
import com.xiaoying.studio.identity.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /**
     * 单文件上传。前端点选图片后立刻上传，拿到 assetId 再随任务一起提交，
     * 因此提交接口只处理 JSON，不需要再等文件流。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse upload(@RequestParam("file") MultipartFile file, @CurrentUser AppUser user) {
        return assetService.upload(file, user);
    }
}
