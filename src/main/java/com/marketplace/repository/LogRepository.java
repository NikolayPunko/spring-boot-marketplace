package com.marketplace.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class LogRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // audit_log: фильтр по датам + table_name + action + лимит
    public List<Map<String, Object>> audit(
            LocalDate start,
            LocalDate end,
            String tableName,
            String action,
            int limit
    ) {
        String sql = """
                SELECT
                  a.id,
                  a.action_time AS created_at,
                  a.table_name,
                  a.action,
                  a.user_id,
                  u.email AS user_email
                FROM audit_log a
                LEFT JOIN users u ON u.id = a.user_id
                WHERE (:start IS NULL OR a.action_time::date >= :start)
                  AND (:end IS NULL OR a.action_time::date <= :end)
                  AND (:tableName IS NULL OR a.table_name = :tableName)
                  AND (:action IS NULL OR a.action = :action)
                ORDER BY a.action_time DESC
                LIMIT :lim
                """;

        return jdbc.queryForList(sql, Map.of(
                "start", start,
                "end", end,
                "tableName", emptyToNull(tableName),
                "action", emptyToNull(action),
                "lim", limit
        ));
    }

    // login_history: фильтр по датам + userEmail + лимит
    public List<Map<String, Object>> logins(
            LocalDate start,
            LocalDate end,
            String userEmail,
            int limit
    ) {
        String sql = """
                SELECT lh.id,
                       lh.login_time,
                       lh.ip_address,
                       u.id AS user_id,
                       u.email AS user_email
                FROM login_history lh
                JOIN users u ON u.id = lh.user_id
                WHERE (:start IS NULL OR lh.login_time::date >= :start)
                  AND (:end IS NULL OR lh.login_time::date <= :end)
                  AND (:email IS NULL OR u.email ILIKE '%' || :email || '%')
                ORDER BY lh.login_time DESC
                LIMIT :lim
                """;

        return jdbc.queryForList(sql, Map.of(
                "start", start,
                "end", end,
                "email", emptyToNull(userEmail),
                "lim", limit
        ));
    }

    // Списки значений для фильтров (чтобы фронту было удобно)
    public List<Map<String, Object>> auditTableNames() {
        String sql = """
                SELECT DISTINCT table_name
                FROM audit_log
                ORDER BY table_name
                """;
        return jdbc.queryForList(sql, Map.of());
    }

    public List<Map<String, Object>> auditActions() {
        String sql = """
                SELECT DISTINCT action
                FROM audit_log
                ORDER BY action
                """;
        return jdbc.queryForList(sql, Map.of());
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}