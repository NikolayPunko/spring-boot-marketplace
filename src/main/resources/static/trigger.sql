-- =========================================================
-- TRIGGERS FOR MARKETPLACE DATABASE
-- =========================================================

-- Удаляем триггеры если уже существуют
DROP TRIGGER IF EXISTS trg_update_order_total ON order_items;
DROP TRIGGER IF EXISTS trg_decrease_stock ON order_items;
DROP TRIGGER IF EXISTS trg_check_stock ON products;
DROP TRIGGER IF EXISTS trg_update_seller_rating ON product_reviews;
DROP TRIGGER IF EXISTS trg_log_orders ON orders;
DROP TRIGGER IF EXISTS trg_set_product_created ON products;
DROP TRIGGER IF EXISTS trg_log_login ON login_history;

-- Удаляем функции если уже существуют
DROP FUNCTION IF EXISTS update_order_total();
DROP FUNCTION IF EXISTS decrease_product_stock();
DROP FUNCTION IF EXISTS check_stock_not_negative();
DROP FUNCTION IF EXISTS update_seller_rating();
DROP FUNCTION IF EXISTS log_order_changes();
DROP FUNCTION IF EXISTS set_product_created_at();
DROP FUNCTION IF EXISTS log_user_login();

-- =========================================================
-- 1. Пересчёт суммы заказа
-- =========================================================

CREATE OR REPLACE FUNCTION update_order_total()
    RETURNS TRIGGER AS $$
BEGIN
UPDATE orders
SET total_amount = COALESCE(
        (SELECT SUM(quantity * price)
         FROM order_items
         WHERE order_id = NEW.order_id), 0)
WHERE id = NEW.order_id;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_order_total
    AFTER INSERT OR UPDATE OR DELETE
                    ON order_items
                        FOR EACH ROW
                        EXECUTE FUNCTION update_order_total();


-- =========================================================
-- 2. Уменьшение остатка товара
-- =========================================================

CREATE OR REPLACE FUNCTION decrease_product_stock()
    RETURNS TRIGGER AS $$
BEGIN
UPDATE products
SET stock_quantity = stock_quantity - NEW.quantity
WHERE id = NEW.product_id;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_decrease_stock
    AFTER INSERT ON order_items
    FOR EACH ROW
    EXECUTE FUNCTION decrease_product_stock();


-- =========================================================
-- 3. Проверка отрицательного остатка
-- =========================================================

CREATE OR REPLACE FUNCTION check_stock_not_negative()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.stock_quantity < 0 THEN
        RAISE EXCEPTION 'Stock cannot be negative!';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_stock
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION check_stock_not_negative();


-- =========================================================
-- 4. Пересчёт рейтинга продавца
-- =========================================================

CREATE OR REPLACE FUNCTION update_seller_rating()
    RETURNS TRIGGER AS $$
DECLARE
sellerId INT;
BEGIN
SELECT seller_id INTO sellerId
FROM products
WHERE id = NEW.product_id;

UPDATE sellers
SET rating = (
    SELECT COALESCE(AVG(rating), 0)
    FROM product_reviews pr
             JOIN products p ON pr.product_id = p.id
    WHERE p.seller_id = sellerId
)
WHERE id = sellerId;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_seller_rating
    AFTER INSERT OR UPDATE ON product_reviews
                        FOR EACH ROW
                        EXECUTE FUNCTION update_seller_rating();


-- =========================================================
-- 5. Логирование изменений заказов
-- =========================================================

CREATE OR REPLACE FUNCTION log_order_changes()
    RETURNS TRIGGER AS $$
BEGIN
INSERT INTO audit_log(user_id, action, table_name)
VALUES (NEW.user_id, TG_OP, 'orders');

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_orders
    AFTER INSERT OR UPDATE OR DELETE
                    ON orders
                        FOR EACH ROW
                        EXECUTE FUNCTION log_order_changes();


-- =========================================================
-- 6. Автоматическая установка created_at
-- =========================================================

CREATE OR REPLACE FUNCTION set_product_created_at()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.created_at IS NULL THEN
        NEW.created_at := CURRENT_TIMESTAMP;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_product_created
    BEFORE INSERT ON products
    FOR EACH ROW
    EXECUTE FUNCTION set_product_created_at();


-- =========================================================
-- 7. Логирование входа пользователя
-- =========================================================

CREATE OR REPLACE FUNCTION log_user_login()
    RETURNS TRIGGER AS $$
BEGIN
INSERT INTO system_logs(event_type, description)
VALUES ('LOGIN', 'User ID: ' || NEW.user_id || ' logged in.');

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_login
    AFTER INSERT ON login_history
    FOR EACH ROW
    EXECUTE FUNCTION log_user_login();

-- =========================================================
-- END OF FILE
-- =========================================================