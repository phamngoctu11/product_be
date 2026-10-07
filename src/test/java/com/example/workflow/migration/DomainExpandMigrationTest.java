package com.example.workflow.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DomainExpandMigrationTest {
    private Connection legacyDatabase() throws SQLException {
        Connection connection = openConnection();
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (id VARCHAR(36) PRIMARY KEY)");
            statement.execute("CREATE TABLE user_vouchers (id BIGINT PRIMARY KEY)");
            statement.execute("CREATE TABLE products (id BIGINT PRIMARY KEY, price DOUBLE)");
            statement.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, status " + legacyStatusType() + ", payment_method VARCHAR(20), final_price DOUBLE)");
            statement.execute("CREATE TABLE orderhistory (id BIGINT PRIMARY KEY, oldstatus " + legacyStatusType() + ", newstatus " + legacyStatusType() + ")");
            statement.execute("CREATE TABLE order_item (id BIGINT PRIMARY KEY, order_id BIGINT, product_variant_id BIGINT, price DOUBLE NOT NULL)");
            statement.execute("INSERT INTO users VALUES ('staff-1')");
            statement.execute("INSERT INTO orders VALUES (1, 'PENDING_PAYMENT', 'online', 123.45), (2, 'DELIVERED', 'ONLINE', 0), (3, 'PENDING_APPROVAL', 'COD', 400)");
            statement.execute("INSERT INTO products VALUES (1, 123.45)");
            statement.execute("INSERT INTO order_item VALUES (1, 1, 1, 123.45)");
        }
        return connection;
    }

    protected Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
    }

    protected String legacyStatusType() { return "VARCHAR(50)"; }

    private void migrate(Connection connection) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        var liquibase = new Liquibase("db/changelog/db.changelog-master.xml", new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }

    @Test
    void migratesLegacyRowsWithoutGuessingPaymentOrDurationAndIsRepeatable() throws Exception {
        try (Connection connection = legacyDatabase()) {
            migrate(connection);
            migrate(connection);
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT * FROM orders ORDER BY id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("status")).isEqualTo("PENDING_PAYMENT");
                assertThat(rows.getString("payment_method_v2")).isEqualTo("ONLINE");
                assertThat(rows.getString("payment_status")).isEqualTo("PENDING");
                assertThat(rows.getDouble("final_price")).isEqualTo(123.45);
                assertThat(rows.getLong("version")).isZero();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("payment_status")).isNull();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("payment_status")).isEqualTo("NOT_DUE");
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT made_day FROM products")) {
                rows.next();
                assertThat(rows.getObject(1)).isNull();
            }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT submitted_at FROM custom_requests WHERE 1 = 0")) {
                assertThat(rows.next()).isFalse();
            }
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE order_item SET price = NULL WHERE id = 1");
                statement.executeUpdate("UPDATE orders SET status = 'ORDER_ACCEPTED' WHERE id = 3");
                statement.executeUpdate("INSERT INTO orderhistory VALUES (1, 'ORDER_ACCEPTED', 'ORDER_CREATING')");
                assertThatThrownBy(() -> statement.executeUpdate("UPDATE products SET made_day = 1.99"))
                        .isInstanceOf(SQLException.class);
                statement.executeUpdate("UPDATE products SET made_day = 2");
            }
        }
    }

    @Test
    void onlyOneActiveOrderCanOccupyStaffAndReleasedAssignmentKeepsHistory() throws Exception {
        try (Connection connection = legacyDatabase()) {
            migrate(connection);
            String insert = "INSERT INTO order_assignments (order_id, staff_id, source, assigned_at, status, active_order_id, active_staff_id) VALUES (%d, 'staff-1', 'SELF_CLAIM', CURRENT_TIMESTAMP, 'ACTIVE', %d, 'staff-1')";
            try (var statement = connection.createStatement()) {
                statement.executeUpdate(insert.formatted(1, 1));
                assertThatThrownBy(() -> statement.executeUpdate(insert.formatted(2, 2))).isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> statement.executeUpdate("UPDATE order_assignments SET active_staff_id = NULL")).isInstanceOf(SQLException.class);
                statement.executeUpdate("UPDATE order_assignments SET status = 'RELEASED', active_order_id = NULL, active_staff_id = NULL, released_at = CURRENT_TIMESTAMP, release_reason = 'FINAL_KCS_PASSED'");
                statement.executeUpdate(insert.formatted(2, 2));
                try (var rows = statement.executeQuery("SELECT COUNT(*) FROM order_assignments")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(2);
                }
            }
        }
    }

    @Test
    void paymentProviderReferenceCannotBeConsumedTwice() throws Exception {
        try (Connection connection = legacyDatabase()) {
            migrate(connection);
            String insert = "INSERT INTO payment_attempts (order_id, provider, request_reference, provider_transaction_id, expected_amount, status, opened_at, due_at, reconciliation_required) VALUES (1, 'MOMO', '%s', 'txn-1', 123.45, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE)";
            try (var statement = connection.createStatement()) {
                statement.executeUpdate(insert.formatted("request-1"));
                assertThatThrownBy(() -> statement.executeUpdate(insert.formatted("request-2"))).isInstanceOf(SQLException.class);
            }
        }
    }
}
