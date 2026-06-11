-- Lyže: unikátní číslo, QR kód, technické parametry (typ, délka, váha, stav), historie přes service_history

CREATE TABLE skis (
    id BIGSERIAL PRIMARY KEY,
    qr_code VARCHAR(100) UNIQUE NOT NULL,
    ski_number VARCHAR(50) UNIQUE NOT NULL,
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    length VARCHAR(10) NOT NULL,
    year INTEGER,
    ski_type VARCHAR(50),
    weight_kg DECIMAL(5,2),
    condition ski_condition DEFAULT 'DOBRY',
    status ski_status DEFAULT 'DOSTUPNY',
    location VARCHAR(100),
    notes TEXT,
    last_service_date DATE,
    next_service_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skis_qr_code ON skis(qr_code);
CREATE INDEX idx_skis_ski_number ON skis(ski_number);
CREATE INDEX idx_skis_status ON skis(status);
CREATE INDEX idx_skis_brand ON skis(brand);
