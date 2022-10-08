DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS csrf_checker;
DROP TABLE IF EXISTS tweets;
DROP TABLE user_follow_relationships;

CREATE TABLE users
(
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100),
  old INT,
  hashed_password VARCHAR(256),
  cookie_value VARCHAR(128),
  email VARCHAR(512),
  PRIMARY KEY(id)
);

CREATE TABLE tweets
(
  id INT NOT NULL AUTO_INCREMENT,
  text VARCHAR(256),
  user_id INTEGER,
  is_reply INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id)
);

CREATE TABLE user_follow_relationships
(
  id INT NOT NULL AUTO_INCREMENT,
  following_user_id INTEGER,
  followed_user_id INTEGER,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY(id)
);

CREATE TABLE csrf_checker
(
  id INT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  hashed_key VARCHAR(256),
  PRIMARY KEY(id)
);