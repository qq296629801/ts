package com.ts.platform.contract;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 契约测试基类；子类按 {@code specs/001-ai-image-platform/contracts/openapi.yaml} 扩展。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class OpenApiContractTestBase {
}
