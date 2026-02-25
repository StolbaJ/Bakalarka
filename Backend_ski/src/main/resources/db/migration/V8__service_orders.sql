-- Servisní zakázky: odkaz na lyži, zákazníka, stav, odhad dokončení, přiřazení technikovi

CREATE TABLE service_orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    ski_id BIGINT REFERENCES skis(id) ON DELETE SET NULL,
    status service_task_status DEFAULT 'CEKA',
    priority service_task_priority DEFAULT 'STREDNI',
    estimated_time_hours DECIMAL(5,2),
    actual_time_hours DECIMAL(5,2),
    assigned_to BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    due_date DATE,
    notes TEXT
);

CREATE INDEX idx_service_orders_order_number ON service_orders(order_number);
CREATE INDEX idx_service_orders_customer_id ON service_orders(customer_id);
CREATE INDEX idx_service_orders_ski_id ON service_orders(ski_id);
CREATE INDEX idx_service_orders_status ON service_orders(status);
CREATE INDEX idx_service_orders_assigned_to ON service_orders(assigned_to);
