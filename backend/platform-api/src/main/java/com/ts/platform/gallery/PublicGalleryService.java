package com.ts.platform.gallery;

import com.ts.platform.template.Template;
import com.ts.platform.template.TemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 公开展示图库：仅已上架模版封面，按热度或最新排序。
 */
@Service
public class PublicGalleryService {

    private final TemplateRepository templateRepository;

    public PublicGalleryService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public Map<String, Object> listPublic(int page, int size, String sort) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 48);
        Sort ordering = "latest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "useCount");
        Page<Template> result = templateRepository.searchApproved(
                null, null, PageRequest.of(p - 1, s, ordering));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Template t : result.getContent()) {
            if (t.getCoverImageUrl() == null || t.getCoverImageUrl().isBlank()) {
                continue;
            }
            items.add(Map.of(
                    "id", "tpl-" + t.getId(),
                    "type", "TEMPLATE",
                    "templateId", t.getId(),
                    "title", t.getTitle(),
                    "imageUrl", t.getCoverImageUrl(),
                    "useCount", t.getUseCount()));
        }
        return Map.of(
                "items", items,
                "total", result.getTotalElements(),
                "page", p,
                "size", s,
                "sort", "latest".equalsIgnoreCase(sort) ? "latest" : "hot");
    }
}
