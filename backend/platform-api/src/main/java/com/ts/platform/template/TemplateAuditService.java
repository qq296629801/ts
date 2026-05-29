package com.ts.platform.template;

import com.ts.platform.admin.AuditLog;
import com.ts.platform.admin.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * 模版内容审核。开发环境默认自动通过；命中敏感词则拒绝。
 */
@Service
public class TemplateAuditService {

    private static final Logger log = LoggerFactory.getLogger(TemplateAuditService.class);
    private static final Set<String> BANNED = Set.of("色情", "赌博", "暴力血腥");

    private final boolean autoApproveDev;
    private final AuditLogRepository auditLogRepository;

    public TemplateAuditService(
            @Value("${app.template.audit-auto-approve-dev:true}") boolean autoApproveDev,
            AuditLogRepository auditLogRepository) {
        this.autoApproveDev = autoApproveDev;
        this.auditLogRepository = auditLogRepository;
    }

    public AuditResult audit(Template template) {
        String text = (template.getTitle() + " " + template.getPrompt()).toLowerCase(Locale.ROOT);
        for (String word : BANNED) {
            if (text.contains(word.toLowerCase(Locale.ROOT))) {
                saveLog(template.getId(), "REJECTED", BigDecimal.ONE, "命中敏感词: " + word);
                return AuditResult.rejected("内容不符合平台规范");
            }
        }
        if (autoApproveDev) {
            saveLog(template.getId(), "APPROVED", BigDecimal.ZERO, "开发环境自动通过");
            log.debug("模版 {} 自动审核通过", template.getId());
            return AuditResult.approved();
        }
        saveLog(template.getId(), "PENDING", null, "待人工审核");
        return AuditResult.pending();
    }

    private void saveLog(Long templateId, String result, BigDecimal score, String reason) {
        AuditLog logEntry = new AuditLog();
        logEntry.setTargetType("TEMPLATE");
        logEntry.setTargetId(templateId);
        logEntry.setResult(result);
        logEntry.setScore(score);
        logEntry.setReason(reason);
        auditLogRepository.save(logEntry);
    }

    public record AuditResult(String status, String rejectReason) {
        static AuditResult approved() {
            return new AuditResult("APPROVED", null);
        }

        static AuditResult rejected(String reason) {
            return new AuditResult("REJECTED", reason);
        }

        static AuditResult pending() {
            return new AuditResult("PENDING", null);
        }
    }
}
