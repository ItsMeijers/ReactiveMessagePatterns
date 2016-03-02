package com.itsmeijers.MessageBus

import com.itsmeijers.CompletableApp
import actors.{TradingBus, MarketAnalysisTools, PortfolioManager, StockTrader}
import actors.TradingBus._

class MessageBusApp extends CompletableApp(9) {

   val tradingBus = system.actorOf(TradingBus.props(this, 6), "tradingBus")

   val marketAnalysisTools = system.actorOf(MarketAnalysisTools.props(this, tradingBus), "marketAnalysisTools")

   val portfolioManager = system.actorOf(PortfolioManager.props(this, tradingBus), "portfolioManager")

   val stockTrader = system.actorOf(StockTrader.props(this, tradingBus), "stockTrader")

   this.awaitCanStartNow()

   tradingBus ! Status

   tradingBus ! TradingCommand("ExecuteBuyOrder", ExecuteBuyOrder("p123", "MSFT", 100, Money(31.85)))

   tradingBus ! TradingCommand("ExecuteSellOrder", ExecuteSellOrder("p456", "MSFT", 200, Money(31.80)))

   tradingBus ! TradingCommand("ExecuteBuyOrder", ExecuteBuyOrder("p789", "MSFT", 100, Money(31.83)))

   this.awaitCompletion()

   println("MessageBus: is completed.")

}
