package com.example.workflow.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationCutoverMigrationTest {

    @Test
    void removesRetiredInventorySchemaAddsOperationsIndexAndIsRepeatable() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL"
        )) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE workflow_outbox (
                            event_id VARCHAR(36) PRIMARY KEY,
                            published_at TIMESTAMP,
                            attempts INT NOT NULL,
                            due_at TIMESTAMP NOT NULL
                        )
                        """);
                statement.execute("CREATE TABLE inventory_transaction (id BIGINT PRIMARY KEY)");
                statement.execute("CREATE TABLE product_variants (id BIGINT PRIMARY KEY, quantity INT NOT NULL)");
                statement.execute("""
                        CREATE TABLE orders (
                            id BIGINT PRIMARY KEY,
                            stock_reserved BOOLEAN,
                            stock_deducted BOOLEAN
                        )
                        """);
            }

            apply(connection);
            apply(connection);

            assertThat(tableExists(connection, "INVENTORY_TRANSACTION")).isFalse();
            assertThat(columnExists(connection, "ORDERS", "STOCK_RESERVED")).isFalse();
            assertThat(columnExists(connection, "ORDERS", "STOCK_DEDUCTED")).isFalse();
            assertThat(columnExists(connection, "PRODUCT_VARIANTS", "QUANTITY")).isFalse();
            assertThat(indexExists(connection, "IDX_OUTBOX_OPERATIONS")).isTrue();
        }
    }

    private void apply(java.sql.Connection connection) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        var liquibase = new Liquibase(
                "db/changelog/12-integration-cutover.xml",
                new ClassLoaderResourceAccessor(),
                database
        );
        liquibase.update(new Contexts(), new LabelExpression());
    }

    private boolean tableExists(java.sql.Connection connection, String table) throws Exception {
        try (var rows = connection.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) {
            return rows.next();
        }
    }

    private boolean columnExists(java.sql.Connection connection, String table, String column) throws Exception {
        try (var rows = connection.getMetaData().getColumns(null, null, table, column)) {
            return rows.next();
        }
    }

    private boolean indexExists(java.sql.Connection connection, String index) throws Exception {
        try (var statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME = ?"
        )) {
            statement.setString(1, index);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1) == 1;
            }
        }
    }
}
