INSERT INTO accounts (user_id, balance)
VALUES
(1, 200000),
(2, 200000)
ON CONFLICT (user_id) DO NOTHING;