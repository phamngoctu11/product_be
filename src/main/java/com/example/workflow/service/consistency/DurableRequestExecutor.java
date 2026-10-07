package com.example.workflow.service.consistency;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Supplier;

/** The operation, its result and its outbox writes commit together. Scope must include actor and action. */
@Service
public class DurableRequestExecutor {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public DurableRequestExecutor(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
    }

    public String execute(String scope, String key, String canonicalPayload, Supplier<String> operation) {
        if (scope == null || scope.isBlank() || key == null || key.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.REQUEST_KEY_REQUIRED);
        }
        String identity = hash(scope.length() + ":" + scope + key);
        String payloadHash = hash(Objects.requireNonNull(canonicalPayload));
        return transaction.execute(status -> {
            try {
                jdbc.update("INSERT INTO workflow_requests(request_key,payload_hash,created_at,completed) VALUES (?,?,?,false)",
                        identity, payloadHash, LocalDateTime.now());
            } catch (DuplicateKeyException duplicate) {
                // MySQL/H2 unique-index arbitration waits for the winning transaction to commit or roll back.
            }
            var row = jdbc.queryForObject("SELECT payload_hash,completed,result_json FROM workflow_requests WHERE request_key=? FOR UPDATE",
                    (rs, index) -> new RequestRow(rs.getString(1), rs.getBoolean(2), rs.getString(3)), identity);
            if (!payloadHash.equals(row.payloadHash())) {
                throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.REQUEST_KEY_CONFLICT);
            }
            if (row.completed()) return row.result();
            String result = Objects.requireNonNull(operation.get(), "Persist a JSON result, including JSON null if appropriate");
            jdbc.update("UPDATE workflow_requests SET completed=true,result_json=? WHERE request_key=?", result, identity);
            return result;
        });
    }

    private record RequestRow(String payloadHash, boolean completed, String result) { }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
