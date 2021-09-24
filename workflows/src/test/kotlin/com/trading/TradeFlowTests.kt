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
    fun `accept trade state between 2 parties`() {

        val counterPartyName = counterParty.name.organisation
        println("CounterParty $counterPartyName")

        val createTrade = TradeInitiator(150, 100, "USD", "EUR", counterParty)
        initiatorNode.startFlow(createTrade).toCompletableFuture()

        mockNetwork.waitQuiescent()

        val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)

        var stateA = initiatorNode.services.vaultService.queryBy(TradeState::class.java,queryCriteria).states

        val counterTrade = CounterTradeInitiator(100, 150, "EUR", "USD", initiatorParty)
        counterPartyNode.startFlow(counterTrade)
        mockNetwork.waitQuiescent()
        var stateB = counterPartyNode.services.vaultService.queryBy(TradeState::class.java,queryCriteria).states
        println(stateB)
//
//        var stateA =
//            initiatorNode.services.vaultService.queryBy(
//                TradeState::class.java,
//                queryCriteria
//            ).states.first { it.state.data.tradeStatus == "OrderPlaced" }.state.data
//        println(stateA)
//        var stateB =
//            counterPartyNode.services.vaultService.queryBy(
//                TradeState::class.java,
//                queryCriteria
//            ).states.first { it.state.data.tradeStatus == "OrderPlaced" }.state.data
//
//        assertEquals(stateA, stateB, "Same state should be available in both nodes")
//
//
////        //counter trade
//        val counterTrade = CounterTradeInitiator(100, 150, "EUR", "USD", initiatorParty)
//
//        counterPartyNode.startFlow(counterTrade)
//
//        mockNetwork.waitQuiescent()
//
//        val stateASuccess =
//            initiatorNode.services.vaultService.queryBy(
//                TradeState::class.java,
//                queryCriteria
//            ).states.first { it.state.data.tradeStatus == "OrderFilled"}.state.data
//        val stateBSuccess =
//            counterPartyNode.services.vaultService.queryBy(
//                TradeState::class.java,
//                queryCriteria
//            ).states.first { it.state.data.tradeStatus == "OrderFilled" }.state.data
//
//        assertEquals(stateASuccess, stateBSuccess, "Same state should be available in both nodes")
//
//        assertEquals(stateA.linearId, stateASuccess.linearId)
    }
}