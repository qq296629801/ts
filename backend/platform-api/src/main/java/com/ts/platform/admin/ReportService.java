package com.ts.platform.admin;

import com.ts.platform.pay.PayOrderRepository;
import com.ts.platform.template.TemplateRepository;
import com.ts.platform.user.UserRepository;
import com.ts.platform.image.ImageAssetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final ImageAssetRepository imageRepository;
    private final PayOrderRepository payOrderRepository;
    private final TemplateRepository templateRepository;

    public ReportService(
            UserRepository userRepository,
            ImageAssetRepository imageRepository,
            PayOrderRepository payOrderRepository,
            TemplateRepository templateRepository) {
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
        this.payOrderRepository = payOrderRepository;
        this.templateRepository = templateRepository;
    }

    public Map<String, Object> summary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> m = new HashMap<>();
        m.put("totalUsers", userRepository.count());
        m.put("todayNewUsers", userRepository.countByCreatedAtAfter(todayStart));
        m.put("totalImages", imageRepository.count());
        m.put("todayImages", imageRepository.countByCreatedAtAfter(todayStart));
        m.put("paidOrderCount", payOrderRepository.countByStatus("PAID"));
        m.put("todayRevenue", payOrderRepository.sumAmountByStatusAndPaidAtAfter("PAID", todayStart));
        m.put("approvedTemplates", templateRepository.countByStatus("APPROVED"));
        m.put("pendingTemplates", templateRepository.countByStatus("PENDING"));
        return m;
    }
}
