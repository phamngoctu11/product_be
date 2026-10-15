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
        applyChangelog(connection, "db/changelog/db.changelog-master.xml");
    }

    private void applyChangelog(Connection connection, String changelog) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        var liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
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

    @Test
    void productionCheckpointAttemptsImagesAndDecisionAreImmutableByConstraint() throws Exception {
        try (Connection connection = legacyDatabase()) {
            migrate(connection);
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO production_checkpoints(
                            order_item_id, stage, status, attempt_number, manager_review_required,
                            submitted_by_id, submitted_at, version
                        ) VALUES (1, 'FINAL_PRODUCT', 'PENDING_REVIEW', 1, TRUE,
                                  'staff-1', CURRENT_TIMESTAMP, 0)
                        """);
                assertThatThrownBy(() -> statement.executeUpdate("""
                        INSERT INTO production_checkpoints(
                            order_item_id, stage, status, attempt_number, manager_review_required,
                            submitted_by_id, submitted_at, version
                        ) VALUES (1, 'FINAL_PRODUCT', 'PENDING_REVIEW', 1, TRUE,
                                  'staff-1', CURRENT_TIMESTAMP, 0)
                        """))
                        .isInstanceOf(SQLException.class);

                statement.executeUpdate("""
                        INSERT INTO production_checkpoint_images(
                            checkpoint_id, image_url, display_order, created_at
                        ) VALUES (1, 'https://cdn.example/final.jpg', 0, CURRENT_TIMESTAMP)
                        """);
                assertThatThrownBy(() -> statement.executeUpdate("""
                        INSERT INTO production_checkpoint_images(
                            checkpoint_id, image_url, display_order, created_at
                        ) VALUES (1, 'https://cdn.example/duplicate.jpg', 0, CURRENT_TIMESTAMP)
                        """))
                        .isInstanceOf(SQLException.class);

                statement.executeUpdate("""
                        INSERT INTO production_decisions(
                            checkpoint_id, decision_type, reason, decided_by_id, decided_at
                        ) VALUES (1, 'REWORK_REQUIRED', 'Can lam lai', 'staff-1', CURRENT_TIMESTAMP)
                        """);
                assertThatThrownBy(() -> statement.executeUpdate("""
                        INSERT INTO production_decisions(
                            checkpoint_id, decision_type, decided_by_id, decided_at
                        ) VALUES (1, 'PASS', 'staff-1', CURRENT_TIMESTAMP)
                        """))
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void normalizesLegacyProductionStatusesBeforeNewEnumsAreRead() throws Exception {
        try (Connection connection = legacyDatabase()) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE production_checkpoints (
                            id BIGINT PRIMARY KEY,
                            status VARCHAR(24) NOT NULL,
                            manager_review_required BOOLEAN NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE production_decisions (
                            id BIGINT PRIMARY KEY,
                            decision_type VARCHAR(16) NOT NULL
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO production_checkpoints VALUES
                            (1, 'SUBMITTED', FALSE),
                            (2, 'SUBMITTED', TRUE),
                            (3, 'APPROVED', TRUE),
                            (4, 'REJECTED', TRUE)
                        """);
                statement.executeUpdate("""
                        INSERT INTO production_decisions VALUES
                            (1, 'APPROVED'),
                            (2, 'REJECTED')
                        """);
            }

            migrate(connection);

            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery(
                         "SELECT status FROM production_checkpoints ORDER BY id"
                 )) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("PASSED");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("PENDING_REVIEW");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("PASSED");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("REWORK_REQUIRED");
            }
            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery(
                         "SELECT decision_type FROM production_decisions ORDER BY id"
                 )) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("PASS");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("REWORK_REQUIRED");
            }
        }
    }

    @Test
    void customContentMigrationBackfillsLegacyNullAttachmentsWithoutRenamingColumns() throws Exception {
        try (Connection connection = legacyDatabase()) {
            applyChangelog(connection, "db/changelog/02-domain-expand.xml");
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO custom_requests(
                            owner_id, status, spec, attachments, quantity, created_at, updated_at
                        ) VALUES (
                            'staff-1', 'DRAFT', 'Legacy description', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        )
                        """);
            }
            connection.commit();

            migrate(connection);

            try (var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT spec, attachments FROM custom_requests")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("spec")).isEqualTo("Legacy description");
                assertThat(rows.getString("attachments")).isEqualTo("[]");
            }
            try (var statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate(
                        "UPDATE custom_requests SET attachments = NULL"
                )).isInstanceOf(SQLException.class);
            }
        }
    }
}
