package com.trading.contracts

import com.trading.states.TradeState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

class TradeContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands

        when (command.first().value) {
            is Commands.CreateTrade -> {
                requireThat {
                    "No inputs should be consumed when issuing an Trade." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<TradeState>().single()

                    "Output State should be TradeState class" using (out::class == TradeState::class)
                    "Trade initiator and receiver cannot be same" using (out.tradeInitiator != out.tradeReceiver)

                    "TradeStatus must be OrderPlaced" using(out.tradeStatus == "OrderPlaced")

                    "Sell value must not be negative" using (out.sellValue > 0)
                    "Buy value must not be negative" using (out.buyValue > 0)

                    "Trade buy & sell currency must not be same" using(out.sellCurrency.toLowerCase() != out.buyCurrency.toLowerCase())

                    "Sell Currency must not be null & less than 3 Character" using (out.sellCurrency.length == 3)
                    "Buy Currency must not be null & less than 3 Character" using (out.buyCurrency.length == 3)
                }
            }
            is Commands.CounterTrade -> {
                requireThat {
                    "Only one inputs state should be created." using (tx.inputs.size == 1)
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<TradeState>().single()

                    "Output State should be TradeState class" using (out::class == TradeState::class)
                    "Trade initiator and receiver cannot be same" using (out.tradeInitiator != out.tradeReceiver)

                    "TradeStatus must be OrderPlaced" using(out.tradeStatus == "OrderFilled")

                    "Sell value must not be negative" using (out.sellValue > 0)
                    "Buy value must not be negative" using (out.buyValue > 0)

                    "Sell Currency must not be null & less than 3 Character" using (out.sellCurrency.length == 3)
                    "Buy Currency must not be null & less than 3 Character" using (out.buyCurrency.length == 3)
                }
            }
        }
    }

    companion object {
        const val ID = "com.trading.contracts.TradeContract"
    }

    interface Commands : CommandData {
        class CreateTrade : Commands
        class CounterTrade : Commands
    }
}
