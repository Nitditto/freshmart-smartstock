-- =====================================================================
-- V1: initial schema for SmartStock (owner: Member A)
-- Flyway is the Java counterpart of EF Core Code First Migrations.
-- RULE: never edit an applied migration; add a new Vnn__*.sql instead.
-- =====================================================================

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'STAFF'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE categories (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE suppliers (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    name    VARCHAR(150) NOT NULL,
    phone   VARCHAR(30)  NULL,
    address VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE products (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    sku               VARCHAR(50)    NOT NULL,
    name              VARCHAR(200)   NOT NULL,
    category_id       BIGINT         NOT NULL,
    unit              VARCHAR(30)    NOT NULL,
    price             DECIMAL(18, 2) NOT NULL,
    quantity_in_stock INT            NOT NULL DEFAULT 0,
    min_stock         INT            NOT NULL DEFAULT 0,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    -- Last line of defence: stock can never go negative, even if application logic has a bug.
    CONSTRAINT ck_products_stock_non_negative CHECK (quantity_in_stock >= 0),
    CONSTRAINT ck_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_products_min_stock_non_negative CHECK (min_stock >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_low_stock ON products (is_active, quantity_in_stock, min_stock);

CREATE TABLE stock_receipts (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    code         VARCHAR(30) NOT NULL,
    supplier_id  BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL,
    note         VARCHAR(500) NULL,
    created_by   BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    confirmed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_receipts_code UNIQUE (code),
    CONSTRAINT fk_stock_receipts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_stock_receipts_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_stock_receipts_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_stock_receipts_status_created ON stock_receipts (status, created_at);
CREATE INDEX idx_stock_receipts_created_by ON stock_receipts (created_by);

CREATE TABLE stock_receipt_details (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(18, 2) NOT NULL,
    PRIMARY KEY (id),
    -- A product may appear only once per document
    CONSTRAINT uk_receipt_detail_product UNIQUE (receipt_id, product_id),
    CONSTRAINT fk_receipt_details_receipt FOREIGN KEY (receipt_id) REFERENCES stock_receipts (id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_details_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_receipt_details_quantity CHECK (quantity > 0),
    CONSTRAINT ck_receipt_details_price CHECK (unit_price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_receipt_details_product ON stock_receipt_details (product_id);

CREATE TABLE stock_issues (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(30)  NOT NULL,
    reason       VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    note         VARCHAR(500) NULL,
    created_by   BIGINT       NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    confirmed_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_issues_code UNIQUE (code),
    CONSTRAINT fk_stock_issues_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_stock_issues_reason CHECK (reason IN ('SALE', 'DAMAGED', 'INTERNAL')),
    CONSTRAINT ck_stock_issues_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_stock_issues_status_created ON stock_issues (status, created_at);
CREATE INDEX idx_stock_issues_created_by ON stock_issues (created_by);

CREATE TABLE stock_issue_details (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    issue_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_issue_detail_product UNIQUE (issue_id, product_id),
    CONSTRAINT fk_issue_details_issue FOREIGN KEY (issue_id) REFERENCES stock_issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_details_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_issue_details_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_issue_details_product ON stock_issue_details (product_id);

-- Daily counter used to generate PN-yyyyMMdd-xxx / PX-yyyyMMdd-xxx (resets every day because the key includes the date)
CREATE TABLE document_counters (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    prefix       VARCHAR(5)  NOT NULL,
    counter_date DATE        NOT NULL,
    last_seq     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_document_counters UNIQUE (prefix, counter_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
