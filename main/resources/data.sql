INSERT INTO users(name, old, hashed_password, email)
VALUES 	('Taro', 30, '329435e5e66be809a656af105f42401e', 'hoge@hoge.com'),
		('Jiro', 25, '329435e5e66be809a656af105f42401e', 'hoge@hogehoge.com'),
		('Saburo', 22, '329435e5e66be809a656af105f42401e', 'hogehoge@hoge.com');

INSERT INTO tweets(text, user_id, is_reply, is_retweet)
VALUES  ('Test Tweets', 1, 0, 0);
INSERT INTO tweets(text, user_id, is_reply, is_retweet)
VALUES  ('Test Tweets2', 2, 0, 0);
INSERT INTO tweets(text, user_id, is_reply, is_retweet)
VALUES  ('Test Tweets3', 3, 0, 0);

INSERT INTO user_follow_relationships(following_user_id, followed_user_id)
VALUES (1, 2), (2, 1), (3, 1);