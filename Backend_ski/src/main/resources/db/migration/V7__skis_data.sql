-- Ukázková data lyží
-- qr_code a ski_number se přepíšou v V16 na formát sk000000001

INSERT INTO skis (qr_code, ski_number, brand, model, length, year, ski_type, weight_kg, condition, status, location, notes) VALUES
('TEMP-QR-001', 'TEMP-SKI-001', 'Salomon', 'S/Force 11', '175', 2024, 'sjezdové', 4.2, 'DOBRY', 'V_SERVISU', 'Servisní dílna A', 'Pravidelné broušení hran'),
('TEMP-QR-002', 'TEMP-SKI-002', 'Rossignol', 'Hero Elite', '170', 2024, 'sjezdové', 3.9, 'VYORNY', 'DOSTUPNY', 'Sklad B', 'Nové lyže, první servis'),
('TEMP-QR-003', 'TEMP-SKI-003', 'Atomic', 'Redster X9', '180', 2023, 'sjezdové', 4.1, 'STREDNI', 'REZERVOVANO', 'Rezervace', 'Potřebuje výměnu vosku'),
('TEMP-QR-004', 'TEMP-SKI-004', 'Head', 'Supershape i.Rally', '165', 2024, 'sjezdové', 3.7, 'VYORNY', 'DOSTUPNY', 'Sklad A', NULL),
('TEMP-QR-005', 'TEMP-SKI-005', 'Fischer', 'RC4 The Curv GT', '168', 2023, 'sjezdové', 3.8, 'STREDNI', 'V_SERVISU', 'Servisní dílna B', 'Výměna vosku a broušení');
