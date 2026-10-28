package com.xiaoying.studio.config;

import com.xiaoying.studio.identity.AuthService;
import com.xiaoying.studio.template.Template;
import com.xiaoying.studio.template.TemplateMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐演示数据：3 个演示账号 + 8 个模板。
 *
 * <p>模板数据放在代码里而不是 seed.sql，是为了让 MySQL 与测试用 H2 共用一份数据源，
 * 避免两套 SQL 方言各写一遍还容易漂移。模板表为空时才插入，重复启动不会产生重复数据。
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final TemplateMapper templateMapper;
    private final AuthService authService;

    public DemoDataSeeder(TemplateMapper templateMapper, AuthService authService) {
        this.templateMapper = templateMapper;
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.ensureDemoAccounts();
        if (templateMapper.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Template> templates = List.of(
                new Template("travel-postcard", "旅行明信片", "创意图片", "/api/files/covers/travel-postcard.svg",
                        "把两张旅途照片拼成一张可以寄出的明信片，自动加上留白与手写感边框。",
                        2, "image/jpeg,image/png", 5L * 1024 * 1024, 10, now),
                new Template("retro-film", "复古胶片", "创意图片", "/api/files/covers/retro-film.svg",
                        "单张照片叠加胶片颗粒与暖色偏移，得到 90 年代冲印质感。",
                        1, "image/jpeg,image/png", 8L * 1024 * 1024, 20, now),
                new Template("neon-portrait", "霓虹人像", "人像写真", "/api/files/covers/neon-portrait.svg",
                        "为人像加上霓虹轮廓光与冷暖分离的渐变背景，适合夜间街头风格。",
                        1, "image/jpeg,image/png,image/webp", 10L * 1024 * 1024, 30, now),
                new Template("spring-festival", "新春贺卡", "节日营销", "/api/files/covers/spring-festival.svg",
                        "双图对称排布的新春贺卡，留出祝福语位置，适合门店与社群转发。",
                        2, "image/jpeg,image/png", 5L * 1024 * 1024, 40, now),
                new Template("product-hero", "商品主图", "电商海报", "/api/files/covers/product-hero.svg",
                        "三张素材组合成一张主图，主图居中、细节图分列两侧，突出卖点。",
                        3, "image/jpeg,image/png", 8L * 1024 * 1024, 50, now),
                new Template("grid-collage", "九宫格拼图", "社交媒体", "/api/files/covers/grid-collage.svg",
                        "四张照片均分排布，自动补齐间距，适合朋友圈与小红书的成组发布。",
                        4, "image/jpeg,image/png", 6L * 1024 * 1024, 60, now),
                new Template("story-cover", "竖版故事封面", "社交媒体", "/api/files/covers/story-cover.svg",
                        "9:16 竖版封面，单图铺满并压暗上下留白，适合短视频与故事流。",
                        1, "image/jpeg,image/png", 5L * 1024 * 1024, 70, now),
                new Template("brand-banner", "品牌横幅", "电商海报", "/api/files/covers/brand-banner.svg",
                        "双图横向延展的横幅，左侧主视觉、右侧留出品牌信息区。",
                        2, "image/jpeg,image/png,image/webp", 6L * 1024 * 1024, 80, now));

        templates.forEach(templateMapper::insert);
        log.info("已写入 {} 个演示模板", templates.size());
    }
}
