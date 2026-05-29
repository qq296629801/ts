package com.ts.platform.admin;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_system_config")
public class SystemConfig {

    @Id
    @Column(name = "config_key", length = 64)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public void touch() { this.updatedAt = LocalDateTime.now(); }
}
