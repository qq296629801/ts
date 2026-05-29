package com.ts.platform.admin;

import com.ts.platform.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminConfigService {

    private final SystemConfigRepository configRepository;

    public AdminConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public Map<String, String> getAll() {
        Map<String, String> map = new LinkedHashMap<>();
        configRepository.findAll().forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }

    @Transactional
    public void update(Map<String, String> updates) {
        for (Map.Entry<String, String> e : updates.entrySet()) {
            SystemConfig cfg = configRepository.findById(e.getKey())
                    .orElseThrow(() -> new BusinessException(400, "未知配置项: " + e.getKey()));
            cfg.setConfigValue(e.getValue());
            cfg.touch();
            configRepository.save(cfg);
        }
    }
}
