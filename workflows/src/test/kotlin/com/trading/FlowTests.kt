package com.trading

import com.trading.flows.CounterTradeInitiator
import com.trading.flows.TradeInitiator
import com.trading.flows.ViewSpecificStates
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.corda.core.concurrent.CordaFuture
import java.util.concurrent.Future;
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import kotlin.test.assertEquals


class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var PartyA: StartedMockNode
    private lateinit var PartyB: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters(
                cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("com.trading.contracts"),
                    TestCordapp.findCordapp("com.trading.flows")
                )
            )
        )
        PartyA = network.createPartyNode()
        PartyB = network.createPartyNode()
        val flow = TradeInitiator(150, 100, "USD", "EUR", PartyB.info.singleIdentity())
        val future: Future<SignedTransaction> = PartyB.startFlow(flow)

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `send trade request`() {
        val flow = TradeInitiator(150, 100, "USD", "EUR", PartyB.info.singleIdentity())
        val future: Future<SignedTransaction> = PartyA.startFlow(flow).toCompletableFuture()

        val signedTx = future.getOrThrow()

        for (node in listOf(PartyA, PartyB)) {
            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
        }

        val flow1 = CounterTradeInitiator(100, 150, "EUR", "USD", PartyA.info.singleIdentity())
        val future1: Future<SignedTransaction> = PartyB.startFlow(flow1)

        val signedTx1 = future1.getOrThrow()

        for (node in listOf(PartyA, PartyB)) {
            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
        }
    }

    @Test
    fun `query all USD and From PartyB`() {
        val flow = ViewSpecificStates("USD", PartyB.info.singleIdentity())
        val future: CordaFuture<String> = PartyA.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
    }
}