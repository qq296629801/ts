package com.ts.platform.admin;

import com.ts.platform.common.BusinessException;
import com.ts.platform.template.Template;
import com.ts.platform.template.TemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminTemplateService {

    private final TemplateRepository templateRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminTemplateService(TemplateRepository templateRepository, AuditLogRepository auditLogRepository) {
        this.templateRepository = templateRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Map<String, Object> listPending(int page, int size) {
        Page<Template> result = templateRepository.findByStatus(
                "PENDING", PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Map<String, Object>> items = result.getContent().stream().map(this::toAdminView).toList();
        return Map.of("items", items, "total", result.getTotalElements());
    }

    @Transactional
    public void audit(Long operatorId, Long templateId, String action, String reason) {
        Template t = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "模版不存在"));
        if (!"PENDING".equals(t.getStatus())) {
            throw new BusinessException(400, "模版已处理");
        }
        if ("APPROVE".equalsIgnoreCase(action)) {
            t.setStatus("APPROVED");
            t.setRejectReason(null);
            saveAudit(operatorId, templateId, "APPROVED", reason);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            t.setStatus("REJECTED");
            t.setRejectReason(reason != null ? reason : "不符合规范");
            saveAudit(operatorId, templateId, "REJECTED", t.getRejectReason());
        } else {
            throw new BusinessException(400, "无效操作");
        }
        templateRepository.save(t);
    }

    private void saveAudit(Long operatorId, Long templateId, String result, String reason) {
        AuditLog log = new AuditLog();
        log.setTargetType("TEMPLATE");
        log.setTargetId(templateId);
        log.setResult(result);
        log.setScore(BigDecimal.ZERO);
        log.setReason(reason);
        log.setOperatorId(operatorId);
        auditLogRepository.save(log);
    }

    private Map<String, Object> toAdminView(Template t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("prompt", t.getPrompt());
        m.put("creatorId", t.getCreatorId());
        m.put("status", t.getStatus());
        m.put("createdAt", t.getCreatedAt().toString());
        return m;
    }
}
