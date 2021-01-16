CREATE TABLE IF NOT EXISTS `sprinkle_money_history`(
    `id`                        bigint(20)      NOT NULL    AUTO_INCREMENT,
    `user_id`                   varchar(64)     NOT NULL,
    `room_id`                   varchar(64)     NOT NULL,
    `receiver_count`            int(6)          NOT NULL,
    `amount`                    int(6)          NOT NULL,
    `token_id`                  varchar(3)      NOT NULL,
    `created_at`                timestamp(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE INDEX UDX_TOKEN_ID(`token_id`),
    INDEX IDX_TOKEN_ID_USER_ID(`token_id`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `sprinkle_money_basket`(
    `id`                        bigint(20)      NOT NULL    AUTO_INCREMENT,
    `token_id`                  varchar(3)      NOT NULL,
    `money`                     int(6)          NOT NULL,
    `received_user_id`          varchar(64),
    `created_at`                timestamp(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`                timestamp(3)    NOT NULL    DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE INDEX UDX_TOKEN_ID_RECEIVED_USER_ID(`token_id`, `received_user_id`),
    INDEX IDX_TOKEN_ID(`token_id`)
);
