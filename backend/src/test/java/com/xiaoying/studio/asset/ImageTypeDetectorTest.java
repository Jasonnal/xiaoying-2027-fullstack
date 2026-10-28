package com.xiaoying.studio.asset;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 服务端不能相信 Content-Type，必须按文件头判断真实类型。
 */
class ImageTypeDetectorTest {

    @Test
    @DisplayName("按魔数识别 PNG / JPEG / WebP / GIF")
    void detectsKnownImageTypes() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a'};

        assertThat(AssetService.detectMimeType(png)).isEqualTo("image/png");
        assertThat(AssetService.detectMimeType(jpeg)).isEqualTo("image/jpeg");
        assertThat(AssetService.detectMimeType(webp)).isEqualTo("image/webp");
        assertThat(AssetService.detectMimeType(gif)).isEqualTo("image/gif");
    }

    @Test
    @DisplayName("伪装成图片的可执行文件/文本返回 null")
    void rejectsNonImagePayloads() {
        byte[] script = "#!/bin/sh\necho hi\n".getBytes();
        byte[] pdf = "%PDF-1.7".getBytes();

        assertThat(AssetService.detectMimeType(script)).isNull();
        assertThat(AssetService.detectMimeType(pdf)).isNull();
        assertThat(AssetService.detectMimeType(new byte[0])).isNull();
    }
}
