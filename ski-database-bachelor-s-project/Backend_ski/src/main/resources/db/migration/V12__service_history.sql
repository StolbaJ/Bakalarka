-- Historie servisních zásahů u lyží (technologická data, diagnostika)

CREATE TABLE service_history (
    id BIGSERIAL PRIMARY KEY,
    ski_id BIGINT NOT NULL REFERENCES skis(id) ON DELETE CASCADE,
    order_id BIGINT REFERENCES service_orders(id) ON DELETE SET NULL,
    service_type VARCHAR(100) NOT NULL,
    service_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    technician_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    duration_minutes INTEGER,
    params JSONB
);

CREATE INDEX idx_service_history_ski_id ON service_history(ski_id);
CREATE INDEX idx_service_history_order_id ON service_history(order_id);
CREATE INDEX idx_service_history_service_date ON service_history(service_date);
