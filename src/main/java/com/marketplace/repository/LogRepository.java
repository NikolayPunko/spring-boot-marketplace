package com.marketplace.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
        StringBuilder sql = new StringBuilder("""
                SELECT
                  a.id,
                  a.action_time AS created_at,
                  a.table_name,
                  a.action,
                  a.user_id,
                  u.email AS user_email
                FROM audit_log a
                LEFT JOIN users u ON u.id = a.user_id
                WHERE 1=1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (start != null) {
            sql.append(" AND a.action_time::date >= :start");
            params.addValue("start", start);
        }

        if (end != null) {
            sql.append(" AND a.action_time::date <= :end");
            params.addValue("end", end);
        }

        if (tableName != null && !tableName.trim().isEmpty()) {
            sql.append(" AND a.table_name = :tableName");
            params.addValue("tableName", tableName);
        }

        if (action != null && !action.trim().isEmpty()) {
            sql.append(" AND a.action = :action");
            params.addValue("action", action);
        }

        sql.append(" ORDER BY a.action_time DESC LIMIT :lim");
        params.addValue("lim", Math.min(limit, 1000));

        return jdbc.queryForList(sql.toString(), params);
    }

    // login_history: фильтр по датам + userEmail + лимит
    public List<Map<String, Object>> logins(
            LocalDate start,
            LocalDate end,
            String userEmail,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT lh.id,
                       lh.login_time,
                       lh.ip_address,
                       u.id AS user_id,
                       u.email AS user_email
                FROM login_history lh
                JOIN users u ON u.id = lh.user_id
                WHERE 1=1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (start != null) {
            sql.append(" AND lh.login_time::date >= :start");
            params.addValue("start", start);
        }

        if (end != null) {
            sql.append(" AND lh.login_time::date <= :end");
            params.addValue("end", end);
        }

        if (userEmail != null && !userEmail.trim().isEmpty()) {
            sql.append(" AND u.email ILIKE :email");
            params.addValue("email", "%" + userEmail + "%");
        }

        sql.append(" ORDER BY lh.login_time DESC LIMIT :lim");
        params.addValue("lim", Math.min(limit, 1000));

        return jdbc.queryForList(sql.toString(), params);
    }


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
}