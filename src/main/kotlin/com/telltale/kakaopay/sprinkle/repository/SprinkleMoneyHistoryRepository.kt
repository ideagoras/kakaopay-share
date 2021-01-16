package com.telltale.kakaopay.sprinkle.repository

import com.telltale.kakaopay.sprinkle.repository.model.SprinkleMoneyHistoryEntity
import com.telltale.kakaopay.sprinkle.repository.model.TokenId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SprinkleMoneyHistoryRepository : JpaRepository<SprinkleMoneyHistoryEntity, Long> {

    fun findByTokenId(tokenId: TokenId): SprinkleMoneyHistoryEntity?
}