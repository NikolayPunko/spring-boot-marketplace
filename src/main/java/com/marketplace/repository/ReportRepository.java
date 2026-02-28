package com.marketplace.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // 1) ТОП товаров по продажам
    public List<Map<String, Object>> topProducts(int limit) {
        String sql = """
                SELECT p.id,
                       p.name,
                       SUM(oi.quantity) AS sold_qty,
                       SUM(oi.quantity * oi.price) AS revenue
                FROM order_items oi
                JOIN products p ON p.id = oi.product_id
                JOIN orders o ON o.id = oi.order_id
                WHERE o.status IN ('PAID','DELIVERED')
                GROUP BY p.id, p.name
                ORDER BY sold_qty DESC
                LIMIT :lim
                """;
        return jdbc.queryForList(sql, Map.of("lim", limit));
    }

    // 2) ТОП продавцов по выручке за период
    public List<Map<String, Object>> topSellers(LocalDate start, LocalDate end) {
        String sql = """
                SELECT s.id AS seller_id,
                       s.store_name,
                       SUM(oi.quantity * oi.price) AS revenue,
                       SUM(oi.quantity) AS items_sold
                FROM sellers s
                JOIN products p ON p.seller_id = s.id
                JOIN order_items oi ON oi.product_id = p.id
                JOIN orders o ON o.id = oi.order_id
                WHERE o.status = 'PAID'
                  AND o.created_at::date BETWEEN :start AND :end
                GROUP BY s.id, s.store_name
                ORDER BY revenue DESC
                LIMIT 10
                """;
        return jdbc.queryForList(sql, Map.of("start", start, "end", end));
    }

    // 3) Средний чек по месяцам
    public List<Map<String, Object>> avgCheckByMonth() {
        String sql = """
                SELECT DATE_TRUNC('month', o.created_at) AS month,
                       ROUND(AVG(o.total_amount), 2) AS avg_check,
                       COUNT(*) AS orders_count
                FROM orders o
                WHERE o.status = 'PAID'
                GROUP BY DATE_TRUNC('month', o.created_at)
                ORDER BY month
                """;
        return jdbc.queryForList(sql, Map.of());
    }

    // 4) Выручка по категориям + доля
    public List<Map<String, Object>> categoryRevenueShare() {
        String sql = """
                SELECT c.id AS category_id,
                       c.name AS category_name,
                       SUM(oi.quantity * oi.price) AS revenue,
                       ROUND(
                           100.0 * SUM(oi.quantity * oi.price) /
                           NULLIF(SUM(SUM(oi.quantity * oi.price)) OVER (), 0),
                           2
                       ) AS revenue_share_percent
                FROM categories c
                JOIN products p ON p.category_id = c.id
                JOIN order_items oi ON oi.product_id = p.id
                JOIN orders o ON o.id = oi.order_id
                WHERE o.status = 'PAID'
                GROUP BY c.id, c.name
                ORDER BY revenue DESC
                """;
        return jdbc.queryForList(sql, Map.of());
    }

    // 5) Пользователи без заказов
    public List<Map<String, Object>> usersWithoutOrders() {
        String sql = """
                SELECT u.id, u.email, u.created_at
                FROM users u
                WHERE NOT EXISTS (
                    SELECT 1 FROM orders o WHERE o.user_id = u.id
                )
                ORDER BY u.created_at DESC
                """;
        return jdbc.queryForList(sql, Map.of());
    }

    // 6) Детали заказа (позиции JSON)
    public List<Map<String, Object>> orderDetails(long orderId) {
        String sql = """
                SELECT o.id AS order_id,
                       o.status,
                       o.created_at,
                       o.total_amount,
                       u.email AS buyer_email,
                       COUNT(oi.id) AS items_count,
                       JSON_AGG(
                           JSON_BUILD_OBJECT(
                               'productId', p.id,
                               'name', p.name,
                               'qty', oi.quantity,
                               'price', oi.price
                           ) ORDER BY oi.id
                       ) AS items
                FROM orders o
                JOIN users u ON u.id = o.user_id
                LEFT JOIN order_items oi ON oi.order_id = o.id
                LEFT JOIN products p ON p.id = oi.product_id
                WHERE o.id = :order_id
                GROUP BY o.id, u.email
                """;
        return jdbc.queryForList(sql, Map.of("order_id", orderId));
    }

    // 7) Оплаты по статусам
    public List<Map<String, Object>> paymentsByStatus() {
        String sql = """
                SELECT p.status,
                       COUNT(*) AS payments_count,
                       SUM(p.amount) AS total_amount
                FROM payments p
                GROUP BY p.status
                ORDER BY payments_count DESC
                """;
        return jdbc.queryForList(sql, Map.of());
    }

    // 8) Низкий остаток
    public List<Map<String, Object>> lowStock(int threshold) {
        String sql = """
                SELECT p.id,
                       p.name,
                       p.stock_quantity,
                       s.store_name,
                       c.name AS category_name
                FROM products p
                JOIN sellers s ON s.id = p.seller_id
                JOIN categories c ON c.id = p.category_id
                WHERE p.stock_quantity < :thr
                ORDER BY p.stock_quantity ASC, p.id
                """;
        return jdbc.queryForList(sql, Map.of("thr", threshold));
    }

    // 9) Логины по дням
    public List<Map<String, Object>> loginsByDay(LocalDate start, LocalDate end) {
        String sql = """
                SELECT lh.login_time::date AS day,
                       COUNT(*) AS logins_count
                FROM login_history lh
                WHERE lh.login_time::date BETWEEN :start AND :end
                GROUP BY lh.login_time::date
                ORDER BY day
                """;
        return jdbc.queryForList(sql, Map.of("start", start, "end", end));
    }

    // 10) Сводка audit_log по таблицам/действиям
    public List<Map<String, Object>> auditSummary(LocalDate start, LocalDate end) {
        String sql = """
                SELECT a.table_name,
                       a.action,
                       COUNT(*) AS actions_count,
                       MIN(a.created_at) AS first_event,
                       MAX(a.created_at) AS last_event
                FROM audit_log a
                WHERE a.created_at::date BETWEEN :start AND :end
                GROUP BY a.table_name, a.action
                ORDER BY actions_count DESC
                """;
        return jdbc.queryForList(sql, Map.of("start", start, "end", end));
    }
}