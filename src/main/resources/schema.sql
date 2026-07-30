-- Board game tables
create table if not exists boardgames (
  id         LONG NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(128) NOT NULL UNIQUE,   -- UNIQUE added: prevents duplicate game names
  level      INT NOT NULL,
  minPlayers INT NOT NULL,
  maxPlayers VARCHAR(50) NOT NULL,
  gameType   VARCHAR(50) NOT NULL
);

create table if not exists reviews (
  id     LONG NOT NULL PRIMARY KEY AUTO_INCREMENT,
  gameId LONG NOT NULL,
  text   VARCHAR(1024) NOT NULL UNIQUE
);

alter table reviews
  add constraint if not exists game_review_fk
  foreign key (gameId) references boardgames (id);

-- Spring Security tables — replaces withDefaultSchema()
create table if not exists users (
  username VARCHAR(50)  NOT NULL PRIMARY KEY,
  password VARCHAR(500) NOT NULL,
  enabled  BOOLEAN      NOT NULL
);

create table if not exists authorities (
  username  VARCHAR(50) NOT NULL,
  authority VARCHAR(50) NOT NULL,
  constraint fk_authorities_users foreign key (username) references users (username)
);

create unique index if not exists ix_auth_username
  on authorities (username, authority);