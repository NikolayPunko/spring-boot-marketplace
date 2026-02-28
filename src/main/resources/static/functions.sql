-- =========================================================
-- SQL FUNCTIONS FOR MARKETPLACE DATABASE
-- =========================================================

-- Удаляем если существуют
DROP FUNCTION IF EXISTS get_order_total(INT);
DROP FUNCTION IF EXISTS get_seller_rating(INT);
DROP FUNCTION IF EXISTS get_top_products(INT);
DROP FUNCTION IF EXISTS get_sales_by_period(DATE, DATE);
DROP FUNCTION IF EXISTS get_user_order_count(INT);
DROP FUNCTION IF EXISTS get_low_stock_products(INT);
DROP FUNCTION IF EXISTS get_category_sales(INT);

-- =========================================================
-- 1) Сумма заказа по order_id
-- =========================================================
CREATE OR REPLACE FUNCTION get_order_total(p_order_id INT)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
v_total NUMERIC;
BEGIN
SELECT COALESCE(SUM(quantity * price), 0)
INTO v_total
FROM order_items
WHERE order_id = p_order_id;

RETURN v_total;
END;
$$;

-- =========================================================
-- 2) Рейтинг продавца (средняя оценка по всем его товарам)
-- =========================================================
CREATE OR REPLACE FUNCTION get_seller_rating(p_seller_id INT)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
v_rating NUMERIC;
BEGIN
SELECT COALESCE(AVG(pr.rating), 0)
INTO v_rating
FROM product_reviews pr
         JOIN products p ON pr.product_id = p.id
WHERE p.seller_id = p_seller_id;

RETURN ROUND(v_rating::numeric, 2);
END;
$$;

-- =========================================================
-- 3) Топ товаров по количеству продаж (LIMIT)
--    Возвращает таблицу: product_id, name, sold_qty
-- =========================================================
CREATE OR REPLACE FUNCTION get_top_products(p_limit INT)
RETURNS TABLE(product_id INT, name VARCHAR, sold_qty BIGINT)
LANGUAGE plpgsql
AS $$
BEGIN
RETURN QUERY
SELECT p.id, p.name, SUM(oi.quantity) AS sold_qty
FROM order_items oi
         JOIN products p ON oi.product_id = p.id
         JOIN orders o ON oi.order_id = o.id
WHERE o.status IN ('PAID', 'DELIVERED', 'NEW')  -- можно упростить, но пусть будет
GROUP BY p.id, p.name
ORDER BY sold_qty DESC
    LIMIT p_limit;
END;
$$;

-- =========================================================
-- 4) Продажи за период (по оплаченным заказам)
--    Возвращает total_sales
-- =========================================================
CREATE OR REPLACE FUNCTION get_sales_by_period(p_start DATE, p_end DATE)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
v_sales NUMERIC;
BEGIN
SELECT COALESCE(SUM(total_amount), 0)
INTO v_sales
FROM orders
WHERE status = 'PAID'
  AND created_at::date BETWEEN p_start AND p_end;

RETURN v_sales;
END;
$$;

-- =========================================================
-- 5) Количество заказов пользователя
-- =========================================================
CREATE OR REPLACE FUNCTION get_user_order_count(p_user_id INT)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
v_cnt INT;
BEGIN
SELECT COUNT(*)
INTO v_cnt
FROM orders
WHERE user_id = p_user_id;

RETURN v_cnt;
END;
$$;

-- =========================================================
-- 6) Товары с низким остатком (меньше заданного порога)
--    Возвращает таблицу: id, name, stock_quantity
-- =========================================================
CREATE OR REPLACE FUNCTION get_low_stock_products(p_threshold INT)
RETURNS TABLE(product_id INT, name VARCHAR, stock_quantity INT)
LANGUAGE plpgsql
AS $$
BEGIN
RETURN QUERY
SELECT id, name, stock_quantity
FROM products
WHERE stock_quantity < p_threshold
ORDER BY stock_quantity ASC;
END;
$$;

-- =========================================================
-- 7) Продажи по категории (сумма qty*price)
-- =========================================================
CREATE OR REPLACE FUNCTION get_category_sales(p_category_id INT)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
v_sales NUMERIC;
BEGIN
SELECT COALESCE(SUM(oi.quantity * oi.price), 0)
INTO v_sales
FROM order_items oi
         JOIN products p ON oi.product_id = p.id
         JOIN orders o ON oi.order_id = o.id
WHERE p.category_id = p_category_id
  AND o.status = 'PAID';

RETURN v_sales;
END;
$$;

-- =========================================================
-- END OF FILE
-- =========================================================


    --примеры вызовов

SELECT get_order_total(1);

SELECT get_seller_rating(1);

SELECT * FROM get_top_products(5);

SELECT get_sales_by_period('2026-01-01', '2026-12-31');

SELECT get_user_order_count(3);

SELECT * FROM get_low_stock_products(12);

SELECT get_category_sales(1);