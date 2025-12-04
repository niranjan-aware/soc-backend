package com.omnigaurd.solilos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageConfig {
    private String basePath = "/opt/solilos/repos";
    private Integer cleanupAfterHours = 24;
}
