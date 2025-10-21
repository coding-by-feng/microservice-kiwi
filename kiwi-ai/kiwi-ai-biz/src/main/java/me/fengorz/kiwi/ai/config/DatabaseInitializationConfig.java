package me.fengorz.kiwi.ai.config;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Uses the configured DataSource to:
 * 1. Attempt a connection.
 * 2. If successful, check Liquibase changelog table; run Liquibase if missing.
 */
@Configuration
public class DatabaseInitializationConfig {

    @Bean
    @ConditionalOnBean(DataSource.class) // Only create when a DataSource is present
    ApplicationRunner liquibaseAutoInitializer(DataSource dataSource,
                                               ObjectProvider<SpringLiquibase> liquibaseProvider) {
        return args -> {
            boolean connected = false;
            boolean changeLogTableExists = false;

            try (Connection c = dataSource.getConnection()) {
                connected = true;
                DatabaseMetaData meta = c.getMetaData();
                try (ResultSet rs = meta.getTables(c.getCatalog(), null, "DATABASECHANGELOG", null)) {
                    if (rs.next()) changeLogTableExists = true;
                }
                if (!changeLogTableExists) {
                    try (ResultSet rs = meta.getTables(c.getCatalog(), null, "databasechangelog", null)) {
                        if (rs.next()) changeLogTableExists = true;
                    }
                }
            } catch (SQLException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.toLowerCase().contains("unknown database")) {
                    System.err.println("⚠️  Database does not exist yet. Provide a schema or external init. Message: " + msg);
                } else {
                    System.err.println("⚠️  Could not obtain DB connection at startup: " + msg);
                }
            }

            if (!connected) {
                // Cannot proceed with Liquibase without a valid connection
                return;
            }

            if (!changeLogTableExists) {
                SpringLiquibase liquibase = liquibaseProvider.getIfAvailable();
                if (liquibase == null) {
                    System.err.println("⚠️  SpringLiquibase bean not found; skipping migration.");
                    return;
                }
                try {
                    System.out.println("🚀 Running Liquibase (changelog missing)...");
                    liquibase.afterPropertiesSet();
                    System.out.println("✅ Liquibase initialization complete.");
                } catch (LiquibaseException e) {
                    System.err.println("❌ Liquibase execution failed: " + e.getMessage());
                }
            } else {
                System.out.println("ℹ️  Liquibase changelog table present; no migration needed.");
            }
        };
    }
}

