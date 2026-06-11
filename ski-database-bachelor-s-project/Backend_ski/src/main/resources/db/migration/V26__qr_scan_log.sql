-- Posledních 10 naskenovaných lyží na uživatele (admin/technik) – sync mobil ↔ počítač
CREATE TABLE qr_scan_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ski_id BIGINT NOT NULL REFERENCES skis(id) ON DELETE CASCADE,
    scanned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_qr_scan_log_user_scanned ON qr_scan_log(user_id, scanned_at DESC);
