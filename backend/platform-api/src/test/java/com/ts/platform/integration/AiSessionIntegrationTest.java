package com.ts.platform.integration;

import com.ts.platform.ai.AiSessionService;
import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AiSessionIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AiSessionService sessionService;

    @Test
    void createRenameDeleteSession() {
        User user = createUser("13800138300", "会话集成", 3);
        var created = sessionService.createSession(user.getId(), "CHAT");
        String sessionId = (String) created.get("id");
        assertThat(sessionId).isNotBlank();

        sessionService.renameSession(user.getId(), sessionId, "新标题");
        var list = sessionService.listSessions(user.getId());
        assertThat(list).anyMatch(s -> "新标题".equals(s.get("title")));

        sessionService.deleteSession(user.getId(), sessionId);
        assertThat(sessionService.listSessions(user.getId())).noneMatch(s -> sessionId.equals(s.get("id")));
    }
}
