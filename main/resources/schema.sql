DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS csrf_checker;

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

CREATE TABLE csrf_checker
(
  id INT NOT NULL AUTO_INCREMENT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  hashed_key VARCHAR(256),
  PRIMARY KEY(id)
);