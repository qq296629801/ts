package com.ts.platform.gallery;

import com.ts.platform.admin.SystemConfigRepository;
import com.ts.platform.template.Template;
import com.ts.platform.template.TemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 公开展示图库：已上架模版封面 + 可选精选配置。
 */
@Service
public class PublicGalleryService {

    private static final String FEATURED_KEY = "featured.public_image_ids";

    private final TemplateRepository templateRepository;
    private final SystemConfigRepository configRepository;

    public PublicGalleryService(TemplateRepository templateRepository, SystemConfigRepository configRepository) {
        this.templateRepository = templateRepository;
        this.configRepository = configRepository;
    }

    public Map<String, Object> listPublic(int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 48);
        Page<Template> result = templateRepository.searchApproved(
                null, null, PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "useCount")));

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
        appendFeatured(items);
        return Map.of(
                "items", items,
                "total", result.getTotalElements(),
                "page", p,
                "size", s);
    }

    private void appendFeatured(List<Map<String, Object>> items) {
        configRepository.findById(FEATURED_KEY).ifPresent(cfg -> {
            String raw = cfg.getConfigValue();
            if (raw == null || raw.isBlank()) {
                return;
            }
            Set<String> existing = items.stream()
                    .map(m -> String.valueOf(m.get("id")))
                    .collect(Collectors.toSet());
            for (String part : raw.split(",")) {
                String url = part.trim();
                if (url.isEmpty() || existing.contains("feat-" + url.hashCode())) {
                    continue;
                }
                String id = "feat-" + Math.abs(url.hashCode());
                if (existing.add(id)) {
                    items.add(0, Map.of(
                            "id", id,
                            "type", "FEATURED",
                            "title", "精选",
                            "imageUrl", url));
                }
            }
        });
    }
}
