package com.telltale.kakaopay.sprinkle.repository

import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyBasketEntity
import javassist.compiler.TokenId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface SprinkleMoneyBasketRepository : JpaRepository<SprinkleMoneyBasketEntity, Long> {
    @Modifying
    @Transactional
    @Query(
        value =
            """
            UPDATE ${SprinkleMoneyBasketEntity.TABLE_NAME}
            SET received_user_id = :receivedUserId
            WHERE token_id = :tokenId AND received_user_id IS NULL
            LIMIT 1
            """,
        nativeQuery = true
    )
    fun takeOneSprinkleMoney(tokenId: String, receivedUserId: String)

    fun findByTokenIdAndReceivedUserId(tokenId: String, receivedUserId: String): SprinkleMoneyBasketEntity?

    fun findByTokenId(tokenId: String): List<SprinkleMoneyBasketEntity>
}