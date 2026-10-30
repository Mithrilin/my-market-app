INSERT INTO users (username, password, role)
VALUES
    ('user',  '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', 'ROLE_USER'),
    ('admin', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', 'ROLE_ADMIN')
    ON CONFLICT (username) DO NOTHING;

INSERT INTO items (title, description, img_path, price)
SELECT *
FROM (
         VALUES
             ('Смартфон', 'Современный смартфон с большим экраном', 'images/smartphone.jpg', 45000),
             ('Ноутбук', 'Мощный ноутбук для работы и игр', 'images/laptop.jpg', 85000),
             ('Наушники', 'Беспроводные наушники с шумоподавлением', 'images/headphones.jpg', 12000),
             ('Клавиатура', 'Механическая клавиатура с подсветкой', 'images/keyboard.jpg', 7500),
             ('Мышь', 'Беспроводная мышь с высоким DPI', 'images/mouse.jpg', 3500),
             ('Монитор', 'Монитор 27 дюймов 4K', 'images/monitor.jpg', 32000),
             ('Планшет', 'Планшет для чтения и работы', 'images/tablet.jpg', 25000),
             ('Умные часы', 'Смарт-часы с фитнес-трекером', 'images/watch.jpg', 18000),
             ('Камера', 'Зеркальная камера для профессионалов', 'images/camera.jpg', 65000),
             ('Колонка', 'Портативная Bluetooth-колонка', 'images/speaker.jpg', 5500)
     ) AS seed(title, description, img_path, price)
WHERE NOT EXISTS (SELECT 1 FROM items);