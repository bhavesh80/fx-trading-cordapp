package com.trading.flows

import co.paralleluniverse.fibers.Suspendable
import com.trading.schema.TradeSchemaV1
import com.trading.states.TradeState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder

@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewAllUnComsumedStates : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        val unConsumedStateCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val allUnConsumedStates = serviceHub.vaultService.queryBy<TradeState>(unConsumedStateCriteria).states

        for (i in allUnConsumedStates.indices) {
            println(allUnConsumedStates[i])
        }

        return allUnConsumedStates.toString();
    }
}

@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewSpecificStates(private val buyCurrency: String, private val FromParty: Party) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)

        val results = builder {
            val buyTrade = TradeSchemaV1.PersistentTrade::buyCurrency.equal(buyCurrency)
            val party = TradeSchemaV1.PersistentTrade::tradeReceiver.equal(FromParty)

            println(buyTrade)
            println(party)

            val customCriteria1 = QueryCriteria.VaultCustomQueryCriteria(buyTrade)
            val customCriteria2 = QueryCriteria.VaultCustomQueryCriteria(party)

            val criteria = generalCriteria.and(customCriteria1).and(customCriteria2)
            serviceHub.vaultService.queryBy<TradeState>(criteria).states
        }

        for (i in results.indices) {
            println(results[i].state.data)
        }
        return  results.toString()
    }
}






