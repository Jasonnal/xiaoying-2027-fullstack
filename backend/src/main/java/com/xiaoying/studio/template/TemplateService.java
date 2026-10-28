package com.xiaoying.studio.template;

import com.xiaoying.studio.common.ApiException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

    private final TemplateMapper templateMapper;

    public TemplateService(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public TemplateListResponse list(String category) {
        String normalized = (category == null || category.isBlank() || "all".equalsIgnoreCase(category))
                ? null
                : category.trim();
        List<TemplateResponse> items = templateMapper.findAll(normalized).stream()
                .map(TemplateService::toResponse)
                .toList();
        return new TemplateListResponse(templateMapper.findCategories(), items);
    }

    public TemplateResponse get(String id) {
        Template template = templateMapper.findById(id);
        if (template == null) {
            throw ApiException.notFound("模板不存在或已下架：" + id);
        }
        return toResponse(template);
    }

    /** 任务受理时拿原始模板做服务端校验，不信任前端传来的数量/类型限制。 */
    public Template requireEntity(String id) {
        Template template = templateMapper.findById(id);
        if (template == null) {
            throw ApiException.notFound("模板不存在或已下架：" + id);
        }
        return template;
    }

    private static TemplateResponse toResponse(Template template) {
        List<String> mimeTypes = Arrays.stream(template.acceptedMimeTypes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return new TemplateResponse(
                template.id(),
                template.title(),
                template.category(),
                template.coverUrl(),
                template.description(),
                template.requiredImageCount(),
                mimeTypes,
                template.maxFileSizeBytes());
    }

    public record TemplateResponse(
            String id,
            String title,
            String category,
            String coverUrl,
            String description,
            int requiredImageCount,
            List<String> acceptedMimeTypes,
            long maxFileSizeBytes) {
    }

    public record TemplateListResponse(List<String> categories, List<TemplateResponse> templates) {
    }
}
