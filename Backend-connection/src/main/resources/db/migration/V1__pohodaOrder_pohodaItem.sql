CREATE TABLE pohoda_order (
                              id SERIAL PRIMARY KEY,
                              pohoda_id BIGINT UNIQUE,
                              order_number VARCHAR(50),
                              date_created DATE,
                              date_from DATE,
                              date_to DATE,
                              customer_company VARCHAR(255),
                              customer_name VARCHAR(255),
                              customer_city VARCHAR(100),
                              customer_street VARCHAR(100),
                              customer_zip VARCHAR(20),
                              customer_ico VARCHAR(20),
                              customer_email VARCHAR(100),
                              customer_phone VARCHAR(50),
                              note TEXT,
                              internal_note TEXT,
                              total_price DECIMAL(19, 2)
);

CREATE TABLE pohoda_order_item (
                                   id SERIAL PRIMARY KEY,
                                   order_id INTEGER REFERENCES pohoda_order(id),
                                   product_text VARCHAR(255),
                                   product_code VARCHAR(50),
                                   quantity DECIMAL(19, 2),
                                   unit_price DECIMAL(19, 2),
                                   unit VARCHAR(10)
);