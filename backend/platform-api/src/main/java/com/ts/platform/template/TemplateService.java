package com.ts.platform.template;

import com.ts.platform.common.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateCategoryRepository categoryRepository;
    private final TemplateFavoriteRepository favoriteRepository;
    private final TemplateAuditService auditService;

    public TemplateService(
            TemplateRepository templateRepository,
            TemplateCategoryRepository categoryRepository,
            TemplateFavoriteRepository favoriteRepository,
            TemplateAuditService auditService) {
        this.templateRepository = templateRepository;
        this.categoryRepository = categoryRepository;
        this.favoriteRepository = favoriteRepository;
        this.auditService = auditService;
    }

    public List<Map<String, Object>> categories() {
        return categoryRepository.findByStatusOrderBySortOrderAsc(1).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName()))
                .toList();
    }

    public Map<String, Object> list(Long userId, Long categoryId, String keyword, String sort, int page, int size) {
        Sort s = "hot".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "useCount")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Page<Template> result = templateRepository.searchApproved(
                categoryId,
                blank(keyword) ? null : keyword.trim(),
                PageRequest.of(Math.max(page - 1, 0), size, s));
        List<Map<String, Object>> items = result.getContent().stream()
                .map(t -> toView(t, userId))
                .toList();
        return Map.of("items", items, "total", result.getTotalElements(), "page", page, "size", size);
    }

    @Transactional
    public Map<String, Object> publish(Long userId, Map<String, Object> body) {
        Template t = new Template();
        t.setTitle(require(body, "title"));
        t.setDescription((String) body.get("description"));
        t.setPrompt(require(body, "prompt"));
        t.setCoverImageUrl((String) body.get("coverImageUrl"));
        Object cat = body.get("categoryId");
        if (cat != null) {
            t.setCategoryId(Long.valueOf(cat.toString()));
        }
        t.setSourceType("USER");
        t.setCreatorId(userId);
        templateRepository.save(t);

        TemplateAuditService.AuditResult audit = auditService.audit(t);
        t.setStatus(audit.status());
        if (audit.rejectReason() != null) {
            t.setRejectReason(audit.rejectReason());
        }
        templateRepository.save(t);
        return toView(t, userId);
    }

    @Transactional
    public Map<String, Object> useTemplate(Long userId, Long templateId) {
        Template t = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "模版不存在"));
        if (!"APPROVED".equals(t.getStatus())) {
            throw new BusinessException(400, "模版未上架");
        }
        t.setUseCount(t.getUseCount() + 1);
        templateRepository.save(t);
        return Map.of("prompt", t.getPrompt(), "title", t.getTitle());
    }

    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long templateId) {
        Template t = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "模版不存在"));
        var existing = favoriteRepository.findByUserIdAndTemplateId(userId, templateId);
        boolean liked;
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            t.setLikeCount(Math.max(0, t.getLikeCount() - 1));
            liked = false;
        } else {
            TemplateFavorite fav = new TemplateFavorite();
            fav.setUserId(userId);
            fav.setTemplateId(templateId);
            favoriteRepository.save(fav);
            t.setLikeCount(t.getLikeCount() + 1);
            liked = true;
        }
        templateRepository.save(t);
        return Map.of("liked", liked, "likeCount", t.getLikeCount());
    }

    public Map<String, Object> get(Long userId, Long id) {
        Template t = templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "模版不存在"));
        if (!"APPROVED".equals(t.getStatus()) && !userId.equals(t.getCreatorId())) {
            throw new BusinessException(404, "模版不存在");
        }
        return toView(t, userId);
    }

    private Map<String, Object> toView(Template t, Long userId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription() != null ? t.getDescription() : "");
        m.put("prompt", t.getPrompt());
        m.put("coverImageUrl", t.getCoverImageUrl() != null ? t.getCoverImageUrl() : "");
        m.put("categoryId", t.getCategoryId());
        m.put("status", t.getStatus());
        m.put("useCount", t.getUseCount());
        m.put("likeCount", t.getLikeCount());
        m.put("createdAt", t.getCreatedAt().toString());
        if (userId != null) {
            m.put("liked", favoriteRepository.existsByUserIdAndTemplateId(userId, t.getId()));
        }
        return m;
    }

    private static String require(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || v.toString().isBlank()) {
            throw new BusinessException(400, key + " 不能为空");
        }
        return v.toString().trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
