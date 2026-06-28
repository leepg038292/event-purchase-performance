CREATE TABLE users
(
    id         BIGSERIAL    NOT NULL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    CONSTRAINT uk_users_email UNIQUE (email)
);
