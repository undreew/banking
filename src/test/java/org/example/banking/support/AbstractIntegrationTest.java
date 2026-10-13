package org.example.banking.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

// One MySQL container, started once per JVM and shared by every subclass (the
// Testcontainers "singleton container" pattern). Started here in a static
// initializer instead of via the @Testcontainers/@Container JUnit lifecycle,
// which stops the container after each test class — that would pay the ~10s
// MySQL boot cost once per class instead of once for the whole suite.
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    static {
        MYSQL.start();
    }
}
