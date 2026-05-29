package com.ts.gateway.arch;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关主代码路径禁止 block()，避免阻塞 Reactor 线程。
 */
class WebFluxBlockGuardTest {

    @Test
    void noBlockInGatewayMainSources() throws Exception {
        Path root = Path.of("src/main/java");
        try (Stream<Path> paths = Files.walk(root)) {
            var offenders = paths
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p).contains(".block(");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(Path::toString)
                    .toList();
            assertThat(offenders).isEmpty();
        }
    }
}
