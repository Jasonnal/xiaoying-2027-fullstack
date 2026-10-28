package com.xiaoying.studio.render;

import com.xiaoying.studio.asset.Asset;
import com.xiaoying.studio.job.Job;
import com.xiaoying.studio.storage.StorageService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * "结果整理"阶段的真实产出：把用户素材拼成一张 1200×800 的成品图并落盘。
 *
 * <p>不接任何 AI 服务，但也不是空转——素材真的被读取、裁切、排布、写出。
 * 画布上的文字刻意只用 ASCII：容器里通常没有中文字体，用中文会渲染成方块。
 */
@Component
public class ResultComposer {

    private static final Logger log = LoggerFactory.getLogger(ResultComposer.class);
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StorageService storageService;

    public ResultComposer(StorageService storageService) {
        this.storageService = storageService;
    }

    public String compose(Job job, List<Asset> assets) {
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            paintBackground(g);
            paintImages(g, assets);
            paintCaption(g, job, assets.size());

            Path target = storageService.resultPath(job.id());
            if (!ImageIO.write(canvas, "jpg", target.toFile())) {
                throw new IllegalStateException("没有可用的 JPEG 编码器");
            }
            log.info("任务 {} 结果已写出：{} ({} bytes)", job.id(), target, Files.size(target));
            return storageService.resultUrl(job.id());
        } catch (IOException e) {
            throw new IllegalStateException("写出结果图失败：" + e.getMessage(), e);
        } finally {
            g.dispose();
        }
    }

    private void paintBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x12, 0x14, 0x19), WIDTH, HEIGHT, new Color(0x1B, 0x1F, 0x27)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 顶部柔光，避免大片纯色显得廉价
        g.setPaint(new GradientPaint(WIDTH / 2f, -220, new Color(0x22, 0xD3, 0xA8, 48),
                WIDTH / 2f, 460, new Color(0x22, 0xD3, 0xA8, 0)));
        g.fillOval(WIDTH / 2 - 520, -300, 1040, 760);
    }

    private void paintImages(Graphics2D g, List<Asset> assets) {
        int count = Math.max(1, assets.size());
        int gap = 24;
        int areaTop = 64;
        int areaHeight = HEIGHT - 64 - 132;
        int areaWidth = WIDTH - 96;

        if (count == 1) {
            drawSlot(g, assets.get(0), 96, areaTop, areaWidth, areaHeight);
        } else if (count == 2) {
            int cellW = (areaWidth - gap) / 2;
            drawSlot(g, assets.get(0), 96, areaTop, cellW, areaHeight);
            drawSlot(g, assets.get(1), 96 + cellW + gap, areaTop, cellW, areaHeight);
        } else {
            int bigW = (int) (areaWidth * 0.62);
            int smallW = areaWidth - bigW - gap;
            int smallH = (areaHeight - gap) / 2;
            drawSlot(g, assets.get(0), 96, areaTop, bigW, areaHeight);
            drawSlot(g, assets.get(1), 96 + bigW + gap, areaTop, smallW, smallH);
            if (count >= 3) {
                drawSlot(g, assets.get(2), 96 + bigW + gap, areaTop + smallH + gap, smallW, smallH);
            }
        }
    }

    /** 按 cover 方式裁切填充到圆角矩形里。 */
    private void drawSlot(Graphics2D g, Asset asset, int x, int y, int w, int h) {
        RoundRectangle2D frame = new RoundRectangle2D.Float(x, y, w, h, 22, 22);

        g.setColor(new Color(0x2A, 0x2F, 0x3A));
        g.fill(frame);

        BufferedImage source = readImage(asset);
        if (source != null) {
            java.awt.Shape oldClip = g.getClip();
            g.setClip(frame);
            double scale = Math.max((double) w / source.getWidth(), (double) h / source.getHeight());
            int drawW = (int) Math.ceil(source.getWidth() * scale);
            int drawH = (int) Math.ceil(source.getHeight() * scale);
            int drawX = x + (w - drawW) / 2;
            int drawY = y + (h - drawH) / 2;
            g.drawImage(source, drawX, drawY, drawW, drawH, null);
            g.setClip(oldClip);
        } else {
            g.setColor(new Color(0x8A, 0x92, 0xA6));
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString("source unavailable", x + 24, y + h / 2);
        }

        g.setColor(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(frame);
    }

    private void paintCaption(Graphics2D g, Job job, int imageCount) {
        int baseY = HEIGHT - 92;

        g.setColor(new Color(0x22, 0xD3, 0xA8));
        g.fillRoundRect(96, baseY - 22, 6, 46, 3, 3);

        g.setColor(new Color(0xF5, 0xF7, 0xFA));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        String title = job.templateId().toUpperCase().replace('-', ' ');
        g.drawString(title, 118, baseY + 4);

        g.setColor(new Color(0x8A, 0x92, 0xA6));
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString("MATERIAL STUDIO  ·  DEMO RENDER  ·  " + imageCount + " SOURCE IMAGE(S)", 118, baseY + 30);

        g.setColor(new Color(0x5C, 0x64, 0x74));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String stamp = LocalDateTime.now().format(STAMP);
        int stampWidth = g.getFontMetrics().stringWidth(stamp);
        g.drawString(stamp, WIDTH - 96 - stampWidth, baseY + 30);
    }

    private BufferedImage readImage(Asset asset) {
        try {
            Path path = storageService.resolve(asset.storagePath());
            if (!Files.exists(path)) {
                return null;
            }
            return ImageIO.read(path.toFile());
        } catch (IOException | RuntimeException e) {
            log.warn("读取素材失败 {}: {}", asset.id(), e.getMessage());
            return null;
        }
    }
}
