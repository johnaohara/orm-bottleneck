package org.acme;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Collections;
import java.util.Map;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {
    private static int POSTGRES_PORT = 5432;

    private final PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer("postgres:13.1")
            .withDatabaseName("quarkus_test")
            .withUsername("quarkus_test")
            .withPassword("quarkus_test")
            .withExposedPorts(POSTGRES_PORT);

    @Override
    public Map<String, String> start() {
        postgres.start();

        System.setProperty("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());

        return Collections.singletonMap("postgres.server", postgres.getDatabaseName());
    }

    @Override
    public void stop() {
        postgres.close();
    }
}
