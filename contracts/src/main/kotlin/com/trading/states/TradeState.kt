package com.trading.states

import com.trading.contracts.TradeContract
import com.trading.schema.TradeSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

@BelongsToContract(TradeContract::class)
data class TradeState(val sellValue: Int,
                      val buyValue: Int,
                      val sellCurrency: String,
                      val buyCurrency: String,
                      val tradeStatus: String,
                      val tradeInitiator: Party,
                      val tradeReceiver: Party,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) :  LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOf(tradeInitiator, tradeReceiver)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is TradeSchemaV1 -> TradeSchemaV1.PersistentTrade(
                this.sellValue,
                this.buyValue,
                this.sellCurrency,
                this.buyCurrency,
                this.tradeInitiator.name.toString(),
                this.tradeReceiver.name.toString(),
                this.tradeStatus,
                this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(TradeSchemaV1)
}

