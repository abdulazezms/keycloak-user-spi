CREATE DATABASE user_store;

-- Create a readonly group to the external store
CREATE ROLE readaccess;

\c user_store

-- Grant the group access to existing tables
GRANT USAGE ON SCHEMA public TO readaccess;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readaccess;

-- Grant access to future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readaccess;

-- Create a user and assign to the read only group
CREATE USER test WITH ENCRYPTED PASSWORD 'test';
GRANT readaccess TO test;

-- Add pgcrypto extension for passwords hashing
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- Create a role entity and insert values for testing
CREATE TABLE role_entity (
                      id VARCHAR(36) PRIMARY KEY,
                      name VARCHAR(255) NOT NULL UNIQUE
);
INSERT INTO role_entity (id, name) VALUES
    (gen_random_uuid()::VARCHAR(36), 'admin'),
    (gen_random_uuid()::VARCHAR(36), 'customer'),
    (gen_random_uuid()::VARCHAR(36), 'developer'),
    (gen_random_uuid()::VARCHAR(36), 'support');


-- Create a user_entity table and insert values for testing
CREATE TABLE user_entity (
                             id VARCHAR(36) PRIMARY KEY,
                             username VARCHAR(255) NOT NULL UNIQUE,
                             email VARCHAR(255) NOT NULL UNIQUE,
                             password VARCHAR(255) NOT NULL,
                             role_id VARCHAR(36) NOT NULL,
                             first_name VARCHAR(255) NOT NULL,
                             last_name VARCHAR(255) NOT NULL,
                             created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000::BIGINT,
                             CONSTRAINT fk_role_entity FOREIGN KEY(role_id) REFERENCES role_entity(id)
);
INSERT INTO user_entity (id, email, username, password, role_id, first_name, last_name) VALUES
    --admin user in the system
    (gen_random_uuid()::VARCHAR(36),
    'admin@myorg.com',
    'admin_username',
    crypt('admin', gen_salt('bf')),
    (SELECT id FROM role_entity WHERE name = 'admin'),
    'Admin_first_name',
    'Admin_last_name'),

    --customer user in the system
    (gen_random_uuid()::VARCHAR(36),
    'customer@myorg.com',
    'customer_username',
    crypt('customer', gen_salt('bf')),
    (SELECT id FROM role_entity WHERE name = 'customer'),
    'Customer_first_name',
    'Customer_last_name'),

    --developer user in the system
    (gen_random_uuid()::VARCHAR(36),
    'developer@myorg.com',
    'developer_username',
    crypt('developer', gen_salt('bf')),
    (SELECT id FROM role_entity WHERE name = 'developer'),
    'Developer_first_name',
    'Developer_last_name'),

    --support user in the system
    (gen_random_uuid()::VARCHAR(36),
    'support@myorg.com',
    'support_username',
    crypt('support', gen_salt('bf')),
    (SELECT id FROM role_entity WHERE name = 'support'),
    'Support_first_name',
    'Support_last_name');