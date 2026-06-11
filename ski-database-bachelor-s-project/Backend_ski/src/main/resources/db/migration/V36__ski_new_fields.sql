-- Nový enum pro použití lyží
CREATE TYPE ski_usage AS ENUM (
    'BEZECKE_KLASIKA',
    'BEZECKE_SKATE',
    'BEZECKE_KLASIKA_SKIN',
    'SJEZDOVE',
    'SKI_ALP'
);

-- Nová pole pro lyže
ALTER TABLE skis
    ADD COLUMN ean        VARCHAR(100),
    ADD COLUMN part_no    VARCHAR(100),
    ADD COLUMN serial_no  VARCHAR(100),
    ADD COLUMN ski_usage  ski_usage;
