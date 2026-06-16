package com.example.postapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * Initializes the database schema (and seed data) on application startup
 * by running SQL scripts from the classpath via JdbcTemplate.
 *
 * <p>This avoids needing psql / postgres client on the Jenkins host or
 * in the Jenkins container — the application self-initializes.</p>
 *
 * <p>Both scripts are idempotent: they bail early if the {@code posts}
 * table already exists. The schema script uses CREATE TABLE IF NOT EXISTS
 * so it is safe to run multiple times; the seed script is gated by a
 * row-count check.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // run before other CommandLineRunners (e.g. TestDataSeeder)
public class SchemaInitializer implements CommandLineRunner {

    private static final String SCHEMA_RESOURCE = "classpath:schema-postgres.sql";
    private static final String DATA_RESOURCE    = "classpath:data.sql";

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    public SchemaInitializer(JdbcTemplate jdbcTemplate, ResourceLoader resourceLoader) {
        this.jdbcTemplate = jdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean postsExists = postsTableExists();
        if (postsExists) {
            log.info("Schema already initialized (posts table exists); skipping SQL scripts");
            return;
        }

        Resource schema = resourceLoader.getResource(SCHEMA_RESOURCE);
        if (!schema.exists()) {
            // schema-postgres.sql is git-tracked, so this should not happen
            // in production. Log loudly so a missing file is noticed.
            log.warn("schema-postgres.sql not found on classpath at {} — relying on JPA "
                   + "and Spring Batch auto-init. Data may be incomplete.", SCHEMA_RESOURCE);
            return;
        }

        log.info("Running schema script: {}", SCHEMA_RESOURCE);
        try (var conn = jdbcTemplate.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn, schema);
        }
        log.info("Schema script complete");

        Resource data = resourceLoader.getResource(DATA_RESOURCE);
        if (data.exists()) {
            log.info("Running seed script: {}", DATA_RESOURCE);
            try (var conn = jdbcTemplate.getDataSource().getConnection()) {
                ScriptUtils.executeSqlScript(conn, data);
            }
            log.info("Seed script complete");
        } else {
            log.info("Seed script not present at {} (skipped)", DATA_RESOURCE);
        }
    }

    private boolean postsTableExists() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
              + "WHERE table_schema = current_schema() AND table_name = 'posts'",
                Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Could not check for posts table: {}", e.getMessage());
            return false;
        }
    }
}
