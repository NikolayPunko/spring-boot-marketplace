-- ==========================
-- 1. Убедимся что роли есть
-- ==========================

INSERT INTO roles (name)
SELECT 'BUYER'
    WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'BUYER');

INSERT INTO roles (name)
SELECT 'SELLER'
    WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SELLER');

INSERT INTO roles (name)
SELECT 'ADMIN'
    WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');


-- ==========================
-- 2. Создаем пользователей
-- ==========================

INSERT INTO users (email, password, created_at)
SELECT 'admin@test.com',
       '$2a$10$7QJ8Q1q0Z1z5pFf9xQvZ6u2k5YQ3v0GmY9Wz2e9bZbV7u6uQ9dM8y',
       NOW()
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@test.com');

INSERT INTO users (email, password, created_at)
SELECT 'seller@test.com',
       '$2a$10$7QJ8Q1q0Z1z5pFf9xQvZ6u2k5YQ3v0GmY9Wz2e9bZbV7u6uQ9dM8y',
       NOW()
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'seller@test.com');

INSERT INTO users (email, password, created_at)
SELECT 'buyer@test.com',
       '$2a$10$7QJ8Q1q0Z1z5pFf9xQvZ6u2k5YQ3v0GmY9Wz2e9bZbV7u6uQ9dM8y',
       NOW()
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'buyer@test.com');


-- ==========================
-- 3. Назначаем роли
-- ==========================

-- ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@test.com'
  AND r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- SELLER
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'seller@test.com'
  AND r.name = 'SELLER'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- BUYER
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'buyer@test.com'
  AND r.name = 'BUYER'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);