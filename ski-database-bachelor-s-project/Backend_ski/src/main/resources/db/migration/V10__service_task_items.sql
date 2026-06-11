-- Jednotlivé servisní úkoly v rámci zakázky

CREATE TABLE service_task_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    task_name VARCHAR(100) NOT NULL,
    task_description TEXT,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_service_task_items_order_id ON service_task_items(order_id);
