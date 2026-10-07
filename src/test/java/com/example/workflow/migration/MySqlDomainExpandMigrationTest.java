package com.example.workflow.migration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opt in with -DmysqlMigrationTests=true; all data lives in disposable Docker databases. */
@Testcontainers
@EnabledIfSystemProperty(named = "mysqlMigrationTests", matches = "true")
class MySqlDomainExpandMigrationTest extends DomainExpandMigrationTest {
    @Container
    final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");

    @Override
    protected Connection openConnection() throws SQLException {
        return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }

    @Override
    protected String legacyStatusType() {
        return "ENUM('PENDING_APPROVAL','PENDING_PAYMENT','PENDING_WAREHOUSE','WAREHOUSE_ASSIGNED','PENDING_KCS','SHIPPING','DELIVERED','CANCELLED')";
    }
}
