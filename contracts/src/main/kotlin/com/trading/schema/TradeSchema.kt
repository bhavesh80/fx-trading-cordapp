package com.trading.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object TradeSchema

object TradeSchemaV1 : MappedSchema(
    schemaFamily = TradeSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentTrade::class.java)) {

    override val migrationResource: String
        get() = "fx-trade.changelog-master";

    @Entity
    @Table(name = "fx_trade_state")
    class PersistentTrade(
        @Column(name = "sellValue")
        var sellValue: Int,

        @Column(name = "buyValue")
        var buyValue: Int,

        @Column(name = "sellCurrency")
        val sellCurrency: String,

        @Column(name = "buyCurrency")
        val buyCurrency: String,

        @Column(name = "tradeInitiator")
        val tradeInitiator: String,

        @Column(name = "tradeReceiver")
        val tradeReceiver: String,

        @Column(name = "tradeStatus")
        var tradeStatus : String,

        @Column(name = "linear_id")
        @Type(type = "uuid-char")
        var linearId: UUID


    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this(
            0,
            0,
            "",
            "",
            "",
            "",
            "",
            UUID.randomUUID()
        )
    }
}