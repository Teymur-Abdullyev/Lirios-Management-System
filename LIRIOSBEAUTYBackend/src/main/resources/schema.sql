-- Auto-heal critical product schema on startup (Render-safe, idempotent)

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    barcode VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    cost_price NUMERIC(10, 2) DEFAULT 0.00,
    stock_qty INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

ALTER TABLE products ADD COLUMN IF NOT EXISTS cost_price NUMERIC(10, 2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock_qty INTEGER;
ALTER TABLE products ADD COLUMN IF NOT EXISTS category VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(30);
ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

UPDATE products
SET cost_price = COALESCE(cost_price, 0.00),
    stock_qty = COALESCE(stock_qty, 0),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW()),
    status = COALESCE(
        status,
        CASE
            WHEN deleted_at IS NOT NULL THEN 'DELETED'
            WHEN COALESCE(stock_qty, 0) = 0 THEN 'OUT_OF_STOCK'
            ELSE 'AVAILABLE'
        END
    );

ALTER TABLE products ALTER COLUMN cost_price SET DEFAULT 0.00;
ALTER TABLE products ALTER COLUMN stock_qty SET DEFAULT 0;
ALTER TABLE products ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE products ALTER COLUMN updated_at SET DEFAULT NOW();
ALTER TABLE products ALTER COLUMN status SET DEFAULT 'AVAILABLE';

ALTER TABLE products ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE products ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE products ALTER COLUMN status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public' AND indexname = 'idx_products_barcode'
    ) THEN
        CREATE INDEX idx_products_barcode ON products (barcode);
    END IF;
END $$;
