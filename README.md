## Sprinkling money
### Develop Environment
* Kotlin Coroutine
* SpringBoot WebFlux, JPA
* assertk(https://github.com/willowtreeapps/assertk)\
* mockk(https://mockk.io/)
* testcontainer mysql (https://www.testcontainers.org/modules/databases/mysql/)

### APIs
|Name| Method Path |
|---|:---:|
|뿌리기 API| POST `/v1/kakao-pay/money/sprinkle`|
|받기 API| POST `/v1/kakao-pay/money/sprinkle/{tokenId}`|
|조회 API| GET `/v1/kakao-pay/money/sprinkle/{tokenId}`|

#### API Definition
```
openapi: 3.0.3
info:
    title: Sprinkle money APIs
    version: 1.0.0
servers:
    -   url: /v1/kakao-pay
paths:
    /money/sprinkle:
        post:
            parameters:
              - name: X-USER-ID
                in: header
                required: true
                schema:
                    type: string
              - name: X-ROOM-ID
                in: header
                required: true
                schema:
                    type: string                
            requestBody:
                required: true
                content:
                    application/json:
                    schema:
                      $ref: '#/components/schemas/SprinkleRequest'
            responses:
                '200':
                    description: >
                        Succeeded in splinke money
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/SprinkleResponse'
                '500':
                    description: Internal error like database occurred.
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/ErrorResponse'
    /money/sprinkle/{tokenId}:
        post:
            parameters:
              - name: X-USER-ID
                in: header
                required: true
                schema:
                    type: string
              - name: X-ROOM-ID
                in: header
                required: true
                schema:
                    type: string
              - name: tokenId
                in: path
                required: true
                schema:
                    type: string
            responses:
                '200':
                    description: >
                        Succeeded in get sprinkled money
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/SprinkleMoneyResponse'
                '404':
                    description: >
                        Cannot find tokenId
                '500':
                    content:
                        description: >
                            code | message
                            100  | You have already receive money
                            101  | Owner cannot take money
                            102  | Expired taking money
                        application/json:
                            schema:
                                $ref: '#/components/schemas/ErrorResponse'
        get:
            parameters:
              - name: X-USER-ID
                in: header
                required: true
                schema:
                    type: string
              - name: X-ROOM-ID
                in: header
                required: true
                schema:
                    type: string
              - name: tokenId
                in: path
                required: true
                schema:
                    type: string
            responses:
                '200':
                    description: >
                        Succeeded in get sprinkled money information
                    content:
                        application/json:
                            schema:
                                $ref: '#/components/schemas/DetailedSprinkleMoneyResponse'
                '401':
                    description: >
                        You are not owner
                '408':
                    description: >
                        Expired token
                
                            
components:
    schemas:
        SprinkleRequest:
            type: object
            required:
                - amount
                - receiver_count
            properties:
                amount:
                    type: integer
                    format: int32
                    minimum: 1
                    maximum: 10000000
                receiver_count:
                    type: integer
                    format: int32
                    minimum: 1
                    maximum: 100
        SprinkleResponse:
            type: object
            required:
                - token_id
            properties:
                token_id:
                    type: string
                    maxLength: 3
        SprinkleMoneyResponse:
            type: object
            required:
                - money
            properties:
                money:
                    type: integer
                    format: int32
        ErrorResponse:
            type: object
            required:
                - code
                - message
            properties:
                code:
                    type: integer
                message:
                    type: String
        DetailedSprinkleMoneyResponse:
            type: object
            required:
                - create_at
                - amount
                - total_received_money
                - received_users
            properties:
                create_at:
                    type: integer
                    format: int64
                    descripted: UTC time created token
                amount:
                    type: integer
                    format: int32
                total_received_money:
                    type: integer
                    format: int32
                received_users:
                    type: array
                    items:
                        type: object
                        schema:
                            $ref: '#/components/schemas/ReceivedUserInfo'
        ReceivedUserInfo:
            type: object
            required:
                - user_id
                - money
            properties:
                user_id:
                    type: string
                money:
                    type: integer
                    format: int32
                    
```

### Repository
|Name|Table| Description |
|---|:---:|:---:|
|History Data|sprinkle_money_history| 뿌리기에 대한 기록 |
|Basket Data|sprinkle_money_basket| 각 뿌리기 분배 기록 |

#### DDL
```
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
    UNIQUE INDEX UDX_TOKEN_ID_RECEIVED_USER_ID(`token_id`, `received_user_id`)
);
```

### Logic
* 뿌리기 API
    * `History Data`를 저장합니다.
    * 받아갈 대상의 숫자만큼 `Basket Data`를 저장합니다.
* 받기 API - 단 한번만 가져가기
    * `Basket Data`에서 이미 받았는지 검사합니다
    * `Basket Data`의 `UDX_TOKEN_ID_RECEIVED_USER_ID`를 통해 한번만 가져가게 합니다.

### 검증
* API
    * SprinkeControllerIT
* DB Query
    * SprinkleMoneyBasketRepositoryIT
    * SprinkleMoneyHistoryRepositoryIT
