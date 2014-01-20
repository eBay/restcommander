-- The user table
CREATE TABLE user (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255),
  `password` VARCHAR(255),
  PRIMARY KEY (`id`)
);
