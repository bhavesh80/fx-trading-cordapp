package com.trading


import com.trading.contracts.TradeContract
import com.trading.flows.CounterTradeInitiator
import com.trading.flows.TradeInitiator
import com.trading.states.TradeState
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.core.singleIdentity
import org.junit.Test
import kotlin.test.assertEquals

class TradeFlowTests : AbstractFlowConfiguration() {
    @Test
    fun `PartyA add Trade 150 USD in exchange of 100 EUR, PartyB submit-Match & Fills the trade`() {
        val createTrade = TradeInitiator(100, 150, "USD", "EUR", counterParty)
        initiatorNode.startFlow(createTrade).toCompletableFuture()
        mockNetwork.waitQuiescent()

        val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)

        var stateA = initiatorNode.services.vaultService.queryBy(TradeState::class.java,queryCriteria).states.first { it.state.data.tradeStatus == "OrderPlaced" }.state.data
        assertEquals(stateA.tradeStatus, "OrderPlaced")

        val counterTrade = CounterTradeInitiator(150, 100, "EUR", "USD", initiatorParty)
        counterPartyNode.startFlow(counterTrade)
        mockNetwork.waitQuiescent()


        var stateB = counterPartyNode.services.vaultService.queryBy(TradeState::class.java,queryCriteria).states.first { it.state.data.tradeStatus == "OrderFilled" }.state.data
        assertEquals(stateB.tradeStatus, "OrderFilled")
    }
    @Test
    fun `PartyB add 2 trade of 150 EUR in-exchange 100 USD ,PartyA submit-Match & Fills the trade`(){
        val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)

        val counterTrade = CounterTradeInitiator(150, 100, "EUR", "USD", initiatorParty)
        counterPartyNode.startFlow(counterTrade).toCompletableFuture()
        mockNetwork.waitQuiescent()

        var stateB = counterPartyNode.services.vaultService.queryBy(TradeState::class.java,queryCriteria).states.first { it.state.data.tradeStatus == "OrderPlaced" }.state.data
        assertEquals(stateB.tradeStatus, "OrderPlaced")

        val createTrade = TradeInitiator(100, 150, "USD", "EUR", counterParty)
        initiatorNode.startFlow(createTrade).toCompletableFuture()
        mockNetwork.waitQuiescent()

        var stateA = initiatorNode.services.vaultService.queryBy(TradeState::class.java,queryCriteria).states.first { it.state.data.tradeStatus == "OrderFilled" }.state.data
        assertEquals(stateA.tradeStatus, "OrderFilled")

    }
}
