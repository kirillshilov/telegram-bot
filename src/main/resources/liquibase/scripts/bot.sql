-- liquibase formatted sql

-- changeSet shilovk : 1
CREATE  TABLE NotificationTask (
id SERIAL PRIMARY KEY,
chatId BIGINT,
massage VARCHAR (100),
notificationDate  TIMESTAMP
)



