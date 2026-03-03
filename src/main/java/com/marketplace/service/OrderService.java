package com.marketplace.service;

import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.model.Seller;
import com.marketplace.model.User;
import com.marketplace.repository.SellerRepository;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;

    // =========================
    // BUYER: СОЗДАНИЕ ЗАКАЗА
    // =========================
    @Transactional
    public Long createOrder(CreateOrderRequest request, Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order items are empty");
        }

        // 1) create_order(INT)
        jdbcTemplate.update("CALL create_order(CAST(? AS INT))", user.getId());

        // 2) id последнего заказа
        Integer orderId = jdbcTemplate.queryForObject(
                "SELECT id FROM orders WHERE user_id = CAST(? AS INT) ORDER BY id DESC LIMIT 1",
                Integer.class,
                user.getId()
        );

        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Order creation failed");
        }

        // 3) add_order_item(INT, INT, INT)
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {

            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order item");
            }

            jdbcTemplate.update(
                    "CALL add_order_item(CAST(? AS INT), CAST(? AS INT), CAST(? AS INT))",
                    orderId,
                    item.getProductId(),
                    item.getQuantity()
            );
        }

        // 4) total_amount (триггер пересчитал)
        BigDecimal totalAmount = jdbcTemplate.queryForObject(
                "SELECT total_amount FROM orders WHERE id = CAST(? AS INT)",
                BigDecimal.class,
                orderId
        );

        if (totalAmount == null) totalAmount = BigDecimal.ZERO;

        // 5) create_payment(INT, NUMERIC) => payments + orders.status='PAID'
        jdbcTemplate.update(
                "CALL create_payment(CAST(? AS INT), ?)",
                orderId,
                totalAmount
        );

        // 6) deliveries: создаём запись (если её ещё нет)
        // адрес можно позже расширить, сейчас минимально "Not specified"
        Integer delCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM deliveries WHERE order_id = CAST(? AS INT)",
                Integer.class,
                orderId
        );

        String address = request.getAddress();
        if (address == null || address.isBlank()) {
            address = "Not specified";
        }

        if (delCnt == null || delCnt == 0) {
            jdbcTemplate.update("""
                    INSERT INTO deliveries(order_id, address, delivery_status)
                    VALUES (CAST(? AS INT), ?, 'PROCESSING')
                    """, orderId, address);
        }

        return orderId.longValue();
    }

    // =========================
    // BUYER: МОИ ЗАКАЗЫ
    // =========================
    public List<Map<String, Object>> getMyOrders(Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return jdbcTemplate.queryForList("""
                SELECT id, status, total_amount, created_at
                FROM orders
                WHERE user_id = CAST(? AS INT)
                ORDER BY created_at DESC
                """, user.getId());
    }

    // =========================
    // BUYER: ДЕТАЛИ МОЕГО ЗАКАЗА (+ delivery)
    // =========================
    public Map<String, Object> getMyOrderDetails(long orderId, Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM orders
                WHERE id = CAST(? AS INT)
                  AND user_id = CAST(? AS INT)
                """, Integer.class, orderId, user.getId());

        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your order");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT o.id AS order_id,
                       o.status,
                       o.created_at,
                       o.total_amount,
                       JSON_AGG(
                           JSON_BUILD_OBJECT(
                               'productId', p.id,
                               'name', p.name,
                               'qty', oi.quantity,
                               'price', oi.price
                           ) ORDER BY oi.id
                       ) AS items
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN products p ON p.id = oi.product_id
                WHERE o.id = CAST(? AS INT)
                GROUP BY o.id
                """, orderId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        Map<String, Object> head = rows.get(0);

        // delivery (если есть)
        List<Map<String, Object>> del = jdbcTemplate.queryForList("""
                SELECT id, address, delivery_status
                FROM deliveries
                WHERE order_id = CAST(? AS INT)
                LIMIT 1
                """, orderId);

        head.put("delivery", del.isEmpty() ? null : del.get(0));
        return head;
    }

    // =========================
    // BUYER: ОТМЕНА ЗАКАЗА (+ delivery)
    // =========================
    @Transactional
    public void cancelMyOrder(long orderId, Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        int updated = jdbcTemplate.update("""
                UPDATE orders
                SET status = 'CANCELLED'
                WHERE id = CAST(? AS INT)
                  AND user_id = CAST(? AS INT)
                  AND status = 'NEW'
                """, orderId, user.getId());

        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can cancel only your NEW orders");
        }

        // синхронизируем delivery
        jdbcTemplate.update("""
                UPDATE deliveries
                SET delivery_status = 'CANCELLED'
                WHERE order_id = CAST(? AS INT)
                """, orderId);
    }

    // =========================
    // SELLER: ЗАКАЗЫ ПО МОИМ ТОВАРАМ
    // =========================
    public List<Map<String, Object>> getSellerOrders(Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SELLER"));

        return jdbcTemplate.queryForList("""
                SELECT DISTINCT o.id,
                                o.status,
                                o.total_amount,
                                o.created_at,
                                u.email AS buyer_email
                FROM orders o
                JOIN users u ON u.id = o.user_id
                JOIN order_items oi ON oi.order_id = o.id
                JOIN products p ON p.id = oi.product_id
                WHERE p.seller_id = CAST(? AS INT)
                ORDER BY o.created_at DESC
                """, seller.getId());
    }

    // =========================
    // ADMIN: ВСЕ ЗАКАЗЫ
    // =========================
    public List<Map<String, Object>> getAllOrders() {
        return jdbcTemplate.queryForList("""
                SELECT o.id,
                       o.status,
                       o.total_amount,
                       o.created_at,
                       u.email AS buyer_email
                FROM orders o
                JOIN users u ON u.id = o.user_id
                ORDER BY o.created_at DESC
                """);
    }

    // =========================
    // ADMIN: ДЕТАЛИ ЛЮБОГО ЗАКАЗА (+ delivery)
    // =========================
    public Map<String, Object> getOrderDetailsAdmin(long orderId) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT o.id AS order_id,
                       o.status,
                       o.created_at,
                       o.total_amount,
                       u.email AS buyer_email,
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
                WHERE o.id = CAST(? AS INT)
                GROUP BY o.id, u.email
                """, orderId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        Map<String, Object> head = rows.get(0);

        List<Map<String, Object>> del = jdbcTemplate.queryForList("""
                SELECT id, address, delivery_status
                FROM deliveries
                WHERE order_id = CAST(? AS INT)
                LIMIT 1
                """, orderId);

        head.put("delivery", del.isEmpty() ? null : del.get(0));
        return head;
    }

    // =========================
    // ADMIN: СМЕНА СТАТУСА (+ синхронизация delivery)
    // =========================
    @Transactional
    public void updateOrderStatus(long orderId, String status) {

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        int updated = jdbcTemplate.update("""
                UPDATE orders
                SET status = ?
                WHERE id = CAST(? AS INT)
                """, status, orderId);

        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        // гарантируем что delivery существует (если по старым заказам нет)
        Integer delCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM deliveries WHERE order_id = CAST(? AS INT)",
                Integer.class,
                orderId
        );
        if (delCnt == null || delCnt == 0) {
            jdbcTemplate.update("""
                    INSERT INTO deliveries(order_id, address, delivery_status)
                    VALUES (CAST(? AS INT), ?, 'PROCESSING')
                    """, orderId, "Not specified");
        }

        // синхронизируем delivery_status по статусу заказа
        // минимальная маппинг-логика
        String deliveryStatus = null;

        if ("DELIVERED".equalsIgnoreCase(status)) deliveryStatus = "DELIVERED";
        if ("CANCELLED".equalsIgnoreCase(status)) deliveryStatus = "CANCELLED";

        // можно добавить SHIPPED/PROCESSING при желании, но минимально достаточно DELIVERED/CANCELLED
        if (deliveryStatus != null) {
            jdbcTemplate.update("""
                    UPDATE deliveries
                    SET delivery_status = ?
                    WHERE order_id = CAST(? AS INT)
                    """, deliveryStatus, orderId);
        }
    }

    // =========================
    // SELLER: ДЕТАЛИ ЗАКАЗА (ТОЛЬКО ЕГО ПОЗИЦИИ) + delivery
    // =========================
    public Map<String, Object> getSellerOrderDetails(long orderId, Authentication auth) {

        Long sellerId = jdbcTemplate.queryForObject("""
                SELECT s.id
                FROM sellers s
                JOIN users u ON u.id = s.user_id
                WHERE u.email = ?
                """, Long.class, auth.getName());

        if (sellerId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a seller");
        }

        Integer cnt = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM order_items oi
                JOIN products p ON p.id = oi.product_id
                WHERE oi.order_id = ? AND p.seller_id = ?
                """, Integer.class, orderId, sellerId);

        if (cnt == null || cnt == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this order");
        }

        Map<String, Object> head = jdbcTemplate.queryForMap("""
                SELECT
                  o.id           AS order_id,
                  o.status       AS status,
                  o.created_at   AS created_at,
                  o.total_amount AS total_amount
                FROM orders o
                WHERE o.id = ?
                """, orderId);

        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                SELECT
                  oi.product_id AS productId,
                  p.name        AS name,
                  oi.quantity   AS qty,
                  oi.price      AS price
                FROM order_items oi
                JOIN products p ON p.id = oi.product_id
                WHERE oi.order_id = ? AND p.seller_id = ?
                ORDER BY oi.id
                """, orderId, sellerId);

        head.put("items", items);

        List<Map<String, Object>> del = jdbcTemplate.queryForList("""
                SELECT id, address, delivery_status
                FROM deliveries
                WHERE order_id = CAST(? AS INT)
                LIMIT 1
                """, orderId);

        head.put("delivery", del.isEmpty() ? null : del.get(0));

        return head;
    }
}