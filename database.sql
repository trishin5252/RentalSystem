-- PostgreSQL. Выполнять в НОВОЙ пустой базе; существующие таблицы не заменяются.
-- psql -X -v ON_ERROR_STOP=1 -U postgres -d rental_system -f database.sql
BEGIN;

CREATE TABLE clients (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL CHECK (length(trim(full_name)) > 0),
    phone VARCHAR(30) NOT NULL CHECK (length(trim(phone)) > 0),
    email VARCHAR(150) NOT NULL UNIQUE CHECK (email LIKE '%@%.%'),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX clients_email_ci ON clients(lower(email));
CREATE TABLE equipment (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL CHECK (length(trim(name)) > 0),
    category VARCHAR(80) NOT NULL CHECK (length(trim(category)) > 0),
    price_per_day NUMERIC(12,2) NOT NULL CHECK (price_per_day > 0),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE rental_requests (
    id SERIAL PRIMARY KEY,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    equipment_id INTEGER NOT NULL REFERENCES equipment(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL CHECK (end_date > start_date),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED','ACTIVE','COMPLETED','CANCELLED','OVERDUE')),
    total_cost NUMERIC(12,2) NOT NULL CHECK (total_cost > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- По принятому правилу один предмет закреплён за одной незакрытой заявкой.
CREATE UNIQUE INDEX one_open_rental_per_equipment ON rental_requests(equipment_id)
    WHERE status IN ('CREATED','ACTIVE','OVERDUE');
CREATE INDEX rentals_client ON rental_requests(client_id);
CREATE INDEX rentals_start_date ON rental_requests(start_date);

INSERT INTO clients(full_name,phone,email) VALUES
('Иванов Иван Иванович','+79000000001','ivanov@example.test'),
('Петрова Анна Сергеевна','+79000000002','petrova@example.test'),
('Сидоров Павел Олегович','+79000000003','sidorov@example.test'),
('Смирнова Мария Игоревна','+79000000004','smirnova@example.test'),
('Кузнецов Алексей Петрович','+79000000005','kuznetsov@example.test');
INSERT INTO equipment(name,category,price_per_day,is_available) VALUES
('Перфоратор Bosch','Электроинструмент',500,false),
('Дрель Makita','Электроинструмент',300,false),
('Сварочный аппарат Ресанта','Сварочное',800,false),
('Лазерный уровень','Измерительное',400,false),
('Бетономешалка','Строительное',1200,false),
('Шлифовальная машина','Электроинструмент',450,true),
('Генератор','Строительное',1500,true),
('Тепловая пушка','Строительное',700,true);
-- Даты относительно дня установки: демонстрационные данные остаются актуальными.
INSERT INTO rental_requests(client_id,equipment_id,start_date,end_date,status,total_cost) VALUES
(1,1,CURRENT_DATE,CURRENT_DATE+3,'CREATED',1500),
(2,2,CURRENT_DATE-1,CURRENT_DATE+2,'ACTIVE',900),
(3,3,CURRENT_DATE-7,CURRENT_DATE-2,'OVERDUE',4000),
(4,4,CURRENT_DATE+2,CURRENT_DATE+4,'CREATED',800),
(5,5,CURRENT_DATE-2,CURRENT_DATE+3,'ACTIVE',6000),
(1,6,CURRENT_DATE-12,CURRENT_DATE-10,'COMPLETED',900),
(2,7,CURRENT_DATE-9,CURRENT_DATE-6,'COMPLETED',4500),
(3,8,CURRENT_DATE-8,CURRENT_DATE-7,'CANCELLED',700),
(4,6,CURRENT_DATE-5,CURRENT_DATE-3,'COMPLETED',900),
(5,7,CURRENT_DATE-4,CURRENT_DATE-2,'CANCELLED',3000);

COMMIT;
