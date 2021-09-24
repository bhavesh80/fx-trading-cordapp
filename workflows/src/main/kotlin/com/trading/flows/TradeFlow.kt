package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.flows.FinalityFlow

import net.corda.core.flows.CollectSignaturesFlow

import net.corda.core.transactions.SignedTransaction

import net.corda.core.flows.FlowSession

import net.corda.core.identity.Party

import com.trading.contracts.TradeContract

import net.corda.core.transactions.TransactionBuilder

import com.trading.states.TradeState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat


@InitiatingFlow
@StartableByRPC
class TradeInitiator(private val sellValue: Int,
                private val buyValue: Int,
                private val sellCurrency: String,
                private val buyCurrency: String,
                private val tradeRequestedParty: Party
                ) : FlowLogic<SignedTransaction>() {



    @Suspendable
    override fun call(): SignedTransaction {

        // obtain reference for notary
        val notary = serviceHub.networkMapCache.notaryIdentities.single() // METHOD 1
        val tradeInitiator = ourIdentity

        //Stage 1

        val tradeState = TradeState(sellValue,buyValue,sellCurrency,buyCurrency,"OrderPlaced",tradeInitiator,tradeRequestedParty)
        val txCommand =Command(TradeContract.Commands.CreateTrade(),tradeState.participants.map { it.owningKey })

        val txBuilder = TransactionBuilder(notary)
                                .addOutputState(tradeState, TradeContract.ID)
                                .addCommand(txCommand)

        //Stage 2 - Verify that the transaction is valid
        txBuilder.verify(serviceHub)

        //Stage 3 - Sign the transaction
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Stage 4 - Send the state to the counterparty, and receive it back with their signature
        val otherPartySession = initiateFlow(tradeRequestedParty)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession) ))

        // Stage 5 - Notarise and record the transaction in both parties' vaults
        return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession)
        ))
    }
}

@InitiatedBy(TradeInitiator::class)
class TradeAcceptor(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an Trade transaction." using (output is TradeState)
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

