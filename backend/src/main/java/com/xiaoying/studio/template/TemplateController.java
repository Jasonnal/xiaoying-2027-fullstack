package com.xiaoying.studio.template;

import com.xiaoying.studio.template.TemplateService.TemplateListResponse;
import com.xiaoying.studio.template.TemplateService.TemplateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /** 模板列表（含分类清单），category 省略或 all 表示全部。 */
    @GetMapping
    public TemplateListResponse list(@RequestParam(required = false) String category) {
        return templateService.list(category);
    }

    @GetMapping("/{id}")
    public TemplateResponse detail(@PathVariable String id) {
        return templateService.get(id);
    }
}
