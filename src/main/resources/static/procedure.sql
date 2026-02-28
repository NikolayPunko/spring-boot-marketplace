-- =========================================================
-- STORED PROCEDURES FOR MARKETPLACE DATABASE
-- =========================================================

-- Удаляем если существуют
DROP PROCEDURE IF EXISTS create_order(INT);
DROP PROCEDURE IF EXISTS add_order_item(INT, INT, INT);
DROP PROCEDURE IF EXISTS cancel_order(INT);
DROP PROCEDURE IF EXISTS create_payment(INT, NUMERIC);
DROP PROCEDURE IF EXISTS generate_seller_payout(INT);
DROP PROCEDURE IF EXISTS clear_old_logs();
DROP PROCEDURE IF EXISTS create_system_log(TEXT, TEXT);

-- =========================================================
-- 1. Создание заказа
-- =========================================================

CREATE OR REPLACE PROCEDURE create_order(p_user_id INT)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO orders(user_id, status)
VALUES (p_user_id, 'NEW');
END;
$$;


-- =========================================================
-- 2. Добавление товара в заказ
-- =========================================================

CREATE OR REPLACE PROCEDURE add_order_item(
    p_order_id INT,
    p_product_id INT,
    p_quantity INT
)
LANGUAGE plpgsql
AS $$
DECLARE
v_price NUMERIC;
BEGIN
SELECT price INTO v_price
FROM products
WHERE id = p_product_id;

INSERT INTO order_items(order_id, product_id, quantity, price)
VALUES (p_order_id, p_product_id, p_quantity, v_price);
END;
$$;


-- =========================================================
-- 3. Отмена заказа
-- =========================================================

CREATE OR REPLACE PROCEDURE cancel_order(p_order_id INT)
LANGUAGE plpgsql
AS $$
BEGIN
UPDATE orders
SET status = 'CANCELLED'
WHERE id = p_order_id;
END;
$$;


-- =========================================================
-- 4. Создание платежа
-- =========================================================

CREATE OR REPLACE PROCEDURE create_payment(
    p_order_id INT,
    p_amount NUMERIC
)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO payments(order_id, amount, status)
VALUES (p_order_id, p_amount, 'PAID');

UPDATE orders
SET status = 'PAID'
WHERE id = p_order_id;
END;
$$;


-- =========================================================
-- 5. Выплата продавцу
-- =========================================================

CREATE OR REPLACE PROCEDURE generate_seller_payout(p_seller_id INT)
LANGUAGE plpgsql
AS $$
DECLARE
v_total NUMERIC;
BEGIN
SELECT SUM(oi.quantity * oi.price)
INTO v_total
FROM order_items oi
         JOIN products p ON oi.product_id = p.id
         JOIN orders o ON oi.order_id = o.id
WHERE p.seller_id = p_seller_id
  AND o.status = 'PAID';

INSERT INTO seller_payouts(seller_id, amount)
VALUES (p_seller_id, COALESCE(v_total, 0));
END;
$$;


-- =========================================================
-- 6. Очистка старых логов (старше 30 дней)
-- =========================================================

CREATE OR REPLACE PROCEDURE clear_old_logs()
LANGUAGE plpgsql
AS $$
BEGIN
DELETE FROM system_logs
WHERE created_at < NOW() - INTERVAL '30 days';
END;
$$;


-- =========================================================
-- 7. Создание записи в system_logs
-- =========================================================

CREATE OR REPLACE PROCEDURE create_system_log(
    p_event_type TEXT,
    p_description TEXT
)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO system_logs(event_type, description)
VALUES (p_event_type, p_description);
END;
$$;

-- =========================================================
-- END OF FILE
-- =========================================================



  --  примеры вызовов

CALL create_order(3);

CALL add_order_item(1, 1, 2);

CALL cancel_order(1);

CALL create_payment(1, 880.00);

CALL generate_seller_payout(1);

CALL clear_old_logs();

CALL create_system_log('TEST', 'Manual log entry');