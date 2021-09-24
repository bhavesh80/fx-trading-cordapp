package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.flows.FinalityFlow

import net.corda.core.flows.CollectSignaturesFlow

import net.corda.core.transactions.SignedTransaction

import java.util.stream.Collectors

import net.corda.core.flows.FlowSession

import net.corda.core.identity.Party

import com.trading.contracts.TradeContract
import com.trading.schema.TradeSchemaV1

import net.corda.core.transactions.TransactionBuilder

import com.trading.states.TradeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.builder

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class CounterTradeInitiator(
    private val sellValue: Int,
    private val buyValue: Int,
    private val sellCurrency: String,
    private val buyCurrency: String,
    private val tradeRequestedParty: Party
) : FlowLogic<SignedTransaction>() {


    @Suspendable
    override fun call(): SignedTransaction {

        // obtain reference for notary
        val notary = serviceHub.networkMapCache.notaryIdentities.single() // METHOD 1
        val counterTradeInititor = ourIdentity

        //Stage 1
        val tradeStateAndRefs = serviceHub.vaultService.queryBy<TradeState>().states
        val inputStateAndRef = tradeStateAndRefs.filter {
            (it.state.data.sellCurrency == buyCurrency) and
                    (it.state.data.buyCurrency == sellCurrency) and
                    (it.state.data.sellValue == buyValue) and
                    (it.state.data.buyValue == sellValue) and
                    (it.state.data.tradeStatus == "OrderPlaced")
        }


        var state: TradeState? = null
        var input: StateAndRef<TradeState>? = null
        var tradeState: TradeState? = null
        var txCommand: Command<TradeContract.Commands.CreateTrade>? = null
        var txCommand1: Command<TradeContract.Commands.CounterTrade>? = null
        var txBuilder: TransactionBuilder? = null
        if (inputStateAndRef.isNotEmpty()) {
            println("-------------Trade Matched - Processing transaction-------------")
            state = inputStateAndRef.first().state.data
            input = inputStateAndRef.first()
            tradeState =
                TradeState(
                    state.sellValue,
                    state.buyValue,
                    state.sellCurrency,
                    state.buyCurrency,
                    "OrderFilled",
                    state.tradeInitiator,
                    state.tradeReceiver
                )
            txCommand1 = Command(TradeContract.Commands.CounterTrade(), tradeState.participants.map { it.owningKey })

            txBuilder = TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(tradeState, TradeContract.ID)
                .addCommand(txCommand1)
        } else {
            println("-------------Creating New Trade--------------")
            tradeState = TradeState(
                sellValue,
                buyValue,
                sellCurrency,
                buyCurrency,
                "OrderPlaced",
                ourIdentity,
                tradeRequestedParty
            )
            txCommand = Command(TradeContract.Commands.CreateTrade(), tradeState.participants.map { it.owningKey })
            txBuilder = TransactionBuilder(notary)
                .addOutputState(tradeState, TradeContract.ID)
                .addCommand(txCommand)

        }

        //Stage 2 - Verify that the transaction is valid
        txBuilder.verify(serviceHub)

        //Stage 3 - Sign the transaction
        val partialSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Stage 4 - Send the state to the counterparty, and receive it back with their signature
        val otherPartySession = initiateFlow(tradeRequestedParty)
        val fullySignedTx = subFlow(
            CollectSignaturesFlow(
                partialSignedTx, setOf(otherPartySession)
            )
        )

        // Stage 5 - Notarise and record the transaction in both parties' vaults
        return subFlow(
            FinalityFlow(
                fullySignedTx,
                setOf(otherPartySession)
            )
        )
    }
}


@InitiatedBy(CounterTradeInitiator::class)
class CounterTradeAcceptor(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction." using (output is TradeState)
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

