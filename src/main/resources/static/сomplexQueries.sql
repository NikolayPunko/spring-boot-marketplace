-- =========================================================
-- 10 COMPLEX QUERIES FOR MARKETPLACE DATABASE
-- Practical for use in the application (reports/admin analytics)
-- =========================================================

-- Q1) Топ-5 товаров по количеству продаж (шт.)
-- Использование: "Популярные товары" (витрина/админ-аналитика)
SELECT
    p.id,
    p.name,
    SUM(oi.quantity) AS sold_qty
FROM order_items oi
         JOIN products p ON p.id = oi.product_id
         JOIN orders o ON o.id = oi.order_id
WHERE o.status IN ('PAID', 'DELIVERED')
GROUP BY p.id, p.name
ORDER BY sold_qty DESC
    LIMIT 5;


-- Q2) Топ продавцов по выручке за период
-- Использование: отчет "Лучшие продавцы", фильтр по датам
SELECT
    s.id AS seller_id,
    s.store_name,
    COALESCE(SUM(oi.quantity * oi.price), 0) AS revenue
FROM sellers s
         JOIN products p ON p.seller_id = s.id
         JOIN order_items oi ON oi.product_id = p.id
         JOIN orders o ON o.id = oi.order_id
WHERE o.status = 'PAID'
  AND o.created_at::date BETWEEN :start_date AND :end_date
GROUP BY s.id, s.store_name
ORDER BY revenue DESC
    LIMIT 10;


-- Q3) Средний чек по месяцам (по оплаченным заказам)
-- Использование: график "Средний чек" (админ-аналитика)
SELECT
    DATE_TRUNC('month', o.created_at) AS month,
    ROUND(AVG(o.total_amount), 2) AS avg_check,
    COUNT(*) AS paid_orders
FROM orders o
WHERE o.status = 'PAID'
GROUP BY DATE_TRUNC('month', o.created_at)
ORDER BY month;


-- Q4) Категории по выручке (с долей в общей выручке) — оконная функция
-- Использование: отчет "Структура продаж" (категории)
SELECT
    c.id AS category_id,
    c.name AS category_name,
    SUM(oi.quantity * oi.price) AS revenue,
    ROUND(
                    100.0 * SUM(oi.quantity * oi.price) / NULLIF(SUM(SUM(oi.quantity * oi.price)) OVER (), 0),
                    2
        ) AS revenue_share_percent
FROM categories c
         JOIN products p ON p.category_id = c.id
         JOIN order_items oi ON oi.product_id = p.id
         JOIN orders o ON o.id = oi.order_id
WHERE o.status = 'PAID'
GROUP BY c.id, c.name
ORDER BY revenue DESC;


-- Q5) Пользователи без заказов (anti-join через NOT EXISTS)
-- Использование: админ-панель (неактивные пользователи)
SELECT
    u.id,
    u.email,
    u.created_at
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.user_id = u.id
)
ORDER BY u.created_at DESC;


-- Q6) Заказы с составом + количеством позиций (агрегация и JSON для удобства API)
-- Использование: "Детали заказа" (в приложении удобно отдавать одним запросом)
SELECT
    o.id AS order_id,
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
                )
                ORDER BY oi.id
        ) AS items
FROM orders o
         JOIN users u ON u.id = o.user_id
         LEFT JOIN order_items oi ON oi.order_id = o.id
         LEFT JOIN products p ON p.id = oi.product_id
WHERE o.id = :order_id
GROUP BY o.id, u.email;


-- Q7) Товары без отзывов (LEFT JOIN + IS NULL)
-- Использование: продавец/админ (товары, требующие продвижения)
SELECT
    p.id,
    p.name,
    s.store_name,
    p.created_at
FROM products p
         JOIN sellers s ON s.id = p.seller_id
         LEFT JOIN product_reviews pr ON pr.product_id = p.id
WHERE pr.id IS NULL
ORDER BY p.created_at DESC;


-- Q8) Товары с низким остатком + продавец + категория
-- Использование: отчет "Низкие остатки" (продавец/админ)
SELECT
    p.id,
    p.name,
    p.stock_quantity,
    s.store_name,
    c.name AS category_name
FROM products p
         JOIN sellers s ON s.id = p.seller_id
         JOIN categories c ON c.id = p.category_id
WHERE p.stock_quantity < :threshold
ORDER BY p.stock_quantity ASC, p.id;


-- Q9) Процент отмененных заказов по пользователям (HAVING + вычисления)
-- Использование: админ-аналитика / подозрительные пользователи
SELECT
    u.id AS user_id,
    u.email,
    COUNT(o.id) AS total_orders,
    SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_orders,
    ROUND(
                    100.0 * SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) / NULLIF(COUNT(o.id), 0),
                    2
        ) AS cancelled_percent
FROM users u
         JOIN orders o ON o.user_id = u.id
GROUP BY u.id, u.email
HAVING COUNT(o.id) >= 2
ORDER BY cancelled_percent DESC, total_orders DESC;


-- Q10) Выручка продавца по месяцам (pivot-like через GROUP BY)
-- Использование: личный кабинет продавца (динамика продаж)
SELECT
    s.id AS seller_id,
    s.store_name,
    DATE_TRUNC('month', o.created_at) AS month,
    COALESCE(SUM(oi.quantity * oi.price), 0) AS revenue,
    COALESCE(SUM(oi.quantity), 0) AS items_sold
FROM sellers s
    JOIN products p ON p.seller_id = s.id
    JOIN order_items oi ON oi.product_id = p.id
    JOIN orders o ON o.id = oi.order_id
WHERE o.status = 'PAID'
  AND s.id = :seller_id
GROUP BY s.id, s.store_name, DATE_TRUNC('month', o.created_at)
ORDER BY month;