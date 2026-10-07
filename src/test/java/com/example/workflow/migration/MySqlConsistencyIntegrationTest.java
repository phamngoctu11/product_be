package com.example.workflow.migration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Testcontainers
@EnabledIfSystemProperty(named="mysqlMigrationTests", matches="true")
class MySqlConsistencyIntegrationTest extends ConsistencyIntegrationTest {
    @Container final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36");
    @Override protected DataSource dataSource() {
        return new DriverManagerDataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    }
}
