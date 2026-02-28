package com.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminQueryService {

    private final JdbcTemplate jdbcTemplate;

    // Ограничение, чтобы не вернуть миллион строк
    private static final int DEFAULT_LIMIT = 200;

    public List<Map<String, Object>> executeSelect(String sql) {

        if (sql == null || sql.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL is empty");
        }

        String normalized = normalize(sql);

        // 1) Только SELECT (или WITH ... SELECT)
        if (!(normalized.startsWith("select ") || normalized.startsWith("with "))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only SELECT queries are allowed");
        }

        // 2) Запрет опасных слов
        if (containsDangerousKeywords(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forbidden SQL keywords detected");
        }

        // 3) Запрет множественных команд
        // (чтобы не было "select ...; drop table ...")
        if (normalized.contains(";")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semicolons are not allowed");
        }

        // 4) Добавляем LIMIT, если пользователь не указал
        String finalSql = addLimitIfMissing(sql, normalized);

        try {
            return jdbcTemplate.queryForList(finalSql);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL error: " + ex.getMessage());
        }
    }

    private String normalize(String sql) {
        return sql.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsDangerousKeywords(String normalized) {
        // запрещаем всё, что может менять БД или быть опасным
        String[] banned = {
                "insert ", "update ", "delete ", "drop ", "alter ", "truncate ", "create ",
                "grant ", "revoke ", "call ", "execute ", "do ",
                "copy ", "vacuum ", "analyze ", "refresh ",
                "set ", "reset ", "comment ", "lock "
        };

        for (String b : banned) {
            if (normalized.contains(b)) return true;
        }
        return false;
    }

    private String addLimitIfMissing(String originalSql, String normalized) {
        // если уже есть limit — не трогаем
        if (normalized.contains(" limit ")) {
            return originalSql;
        }

        // добавим LIMIT аккуратно в конец
        return originalSql.trim() + " LIMIT " + DEFAULT_LIMIT;
    }
}