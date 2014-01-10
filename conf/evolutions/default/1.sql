# --- !Ups

CREATE SEQUENCE game_seq;
CREATE TABLE game (
	id bigint,
	json varchar(4096), 

    PRIMARY KEY (id)
);


# --- !Downs
 
DROP SEQUENCE game_seq;
DROP TABLE game;
