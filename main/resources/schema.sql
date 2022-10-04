DROP TABLE IF EXISTS test_table;

CREATE TABLE test_table
(
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(100),
  old INT,
  hashed_password VARCHAR(256),
  email VARCHAR(512),
  PRIMARY KEY(id)
);