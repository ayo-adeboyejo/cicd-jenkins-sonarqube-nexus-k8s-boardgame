-- -------------------------------------------------------
-- Board game seed data
-- No explicit IDs — let AUTO_INCREMENT assign them
-- -------------------------------------------------------
MERGE INTO boardgames (name, level, minPlayers, maxPlayers, gameType)
KEY(name)
VALUES
('Splendor', 3, 2, '4', 'Strategy Game'),
('Clue',     2, 1, '6', 'Strategy Game'),
('Linkee',   1, 2, '+', 'Trivia Game');

-- -------------------------------------------------------
-- Review seed data
-- Uses subquery to get gameId by name — avoids hardcoded IDs
-- -------------------------------------------------------
MERGE INTO reviews (gameId, text)
KEY(text)
VALUES
(
    (SELECT id FROM boardgames WHERE name = 'Splendor'),
    'A great strategy game. The one who collects 15 points first wins. Calculation skill is required.'
),
(
    (SELECT id FROM boardgames WHERE name = 'Splendor'),
    'Collecting gemstones makes me feel like a wealthy merchant. Highly recommend!'
),
(
    (SELECT id FROM boardgames WHERE name = 'Clue'),
    'A detective game to guess the criminal, weapon, and place of the crime scene. It is more fun with more than 3 players.'
);

-- -------------------------------------------------------
-- Default users — BCrypt encoded passwords
-- bugs  → bunny
-- daffy → duck
-- -------------------------------------------------------
MERGE INTO users (username, password, enabled)
KEY(username)
VALUES
('bugs',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU7WDCJW', TRUE),
('daffy', '$2a$10$8KJTeCBMy.sPVGAGMVKdre7Z9HXbGqfAMhMisOQIQaG7DAqZd5x.2', TRUE);

-- -------------------------------------------------------
-- User authorities
-- -------------------------------------------------------
MERGE INTO authorities (username, authority)
KEY(username, authority)
VALUES
('bugs',  'ROLE_USER'),
('daffy', 'ROLE_USER'),
('daffy', 'ROLE_MANAGER');
