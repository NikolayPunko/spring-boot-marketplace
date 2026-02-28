-- ============================================
--   MARKETPLACE DATABASE
-- ============================================

DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS product_reviews CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS deliveries CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS seller_payouts CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS login_history CASCADE;
DROP TABLE IF EXISTS system_logs CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS sellers CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- ============================================
--   1. ROLES
-- ============================================

CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL
);

-- ============================================
--   2. USERS
-- ============================================

CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--   3. USER_ROLES (Many-to-Many)
-- ============================================

CREATE TABLE user_roles (
                            user_id INT REFERENCES users(id) ON DELETE CASCADE,
                            role_id INT REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

-- ============================================
--   4. SELLERS
-- ============================================

CREATE TABLE sellers (
                         id SERIAL PRIMARY KEY,
                         user_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                         store_name VARCHAR(100) NOT NULL,
                         rating NUMERIC(3,2) DEFAULT 0
);

-- ============================================
--   5. CATEGORIES
-- ============================================

CREATE TABLE categories (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
);

-- ============================================
--   6. PRODUCTS
-- ============================================

CREATE TABLE products (
                          id SERIAL PRIMARY KEY,
                          seller_id INT REFERENCES sellers(id) ON DELETE CASCADE,
                          category_id INT REFERENCES categories(id),
                          name VARCHAR(150) NOT NULL,
                          price NUMERIC(10,2) CHECK (price > 0),
                          stock_quantity INT CHECK (stock_quantity >= 0),
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--   7. PRODUCT_REVIEWS
-- ============================================

CREATE TABLE product_reviews (
                                 id SERIAL PRIMARY KEY,
                                 product_id INT REFERENCES products(id) ON DELETE CASCADE,
                                 user_id INT REFERENCES users(id) ON DELETE CASCADE,
                                 rating INT CHECK (rating BETWEEN 1 AND 5)
);

-- ============================================
--   8. ORDERS
-- ============================================

CREATE TABLE orders (
                        id SERIAL PRIMARY KEY,
                        user_id INT REFERENCES users(id) ON DELETE CASCADE,
                        total_amount NUMERIC(10,2) DEFAULT 0,
                        status VARCHAR(50) DEFAULT 'NEW',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--   9. ORDER_ITEMS
-- ============================================

CREATE TABLE order_items (
                             id SERIAL PRIMARY KEY,
                             order_id INT REFERENCES orders(id) ON DELETE CASCADE,
                             product_id INT REFERENCES products(id),
                             quantity INT CHECK (quantity > 0),
                             price NUMERIC(10,2) NOT NULL
);

-- ============================================
--   10. PAYMENTS
-- ============================================

CREATE TABLE payments (
                          id SERIAL PRIMARY KEY,
                          order_id INT REFERENCES orders(id) ON DELETE CASCADE,
                          amount NUMERIC(10,2),
                          payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          status VARCHAR(50)
);

-- ============================================
--   11. DELIVERIES
-- ============================================

CREATE TABLE deliveries (
                            id SERIAL PRIMARY KEY,
                            order_id INT REFERENCES orders(id) ON DELETE CASCADE,
                            address TEXT,
                            delivery_status VARCHAR(50) DEFAULT 'PROCESSING'
);

-- ============================================
--   12. SELLER_PAYOUTS
-- ============================================

CREATE TABLE seller_payouts (
                                id SERIAL PRIMARY KEY,
                                seller_id INT REFERENCES sellers(id) ON DELETE CASCADE,
                                amount NUMERIC(10,2),
                                payout_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--   13. AUDIT_LOG
-- ============================================

CREATE TABLE audit_log (
                           id SERIAL PRIMARY KEY,
                           user_id INT,
                           action TEXT,
                           table_name TEXT,
                           action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--   14. LOGIN_HISTORY
-- ============================================

CREATE TABLE login_history (
                               id SERIAL PRIMARY KEY,
                               user_id INT REFERENCES users(id) ON DELETE CASCADE,
                               login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               ip_address VARCHAR(50)
);

-- ============================================
--   15. SYSTEM_LOGS
-- ============================================

CREATE TABLE system_logs (
                             id SERIAL PRIMARY KEY,
                             event_type VARCHAR(100),
                             description TEXT,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
--   INDEXES
-- ============================================

CREATE INDEX idx_products_seller ON products(seller_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- ============================================
--   INSERT TEST DATA
-- ============================================

-- Roles
INSERT INTO roles (name) VALUES
                             ('ADMIN'),
                             ('SELLER'),
                             ('BUYER');

-- Users
INSERT INTO users (email, password) VALUES
                                        ('admin@mail.com', '1234'),
                                        ('seller@mail.com', '1234'),
                                        ('buyer1@mail.com', '1234'),
                                        ('buyer2@mail.com', '1234');

-- Assign roles
INSERT INTO user_roles VALUES
                           (1,1),
                           (2,2),
                           (3,3),
                           (4,3);

-- Seller
INSERT INTO sellers (user_id, store_name)
VALUES (2, 'Tech Store');

-- Categories
INSERT INTO categories (name) VALUES
                                  ('Electronics'),
                                  ('Books');

-- Products
INSERT INTO products (seller_id, category_id, name, price, stock_quantity)
VALUES
    (1, 1, 'Laptop', 800.00, 10),
    (1, 1, 'Smartphone', 500.00, 15),
    (1, 2, 'SQL Book', 40.00, 20);

-- Orders
INSERT INTO orders (user_id, status)
VALUES
    (3, 'NEW'),
    (4, 'NEW');

-- Order items
INSERT INTO order_items (order_id, product_id, quantity, price)
VALUES
    (1, 1, 1, 800.00),
    (1, 3, 2, 40.00),
    (2, 2, 1, 500.00);

-- Payments
INSERT INTO payments (order_id, amount, status)
VALUES
    (1, 880.00, 'PAID'),
    (2, 500.00, 'PAID');

-- Deliveries
INSERT INTO deliveries (order_id, address)
VALUES
    (1, 'Moscow'),
    (2, 'Saint Petersburg');