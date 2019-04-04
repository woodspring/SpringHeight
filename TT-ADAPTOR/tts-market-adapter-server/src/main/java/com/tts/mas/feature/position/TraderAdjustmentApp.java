package com.tts.mas.feature.position;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.message.TtMessageStruct.TtMsg;
import com.tts.message.common.CommonStruct.BuySellActionCd;
import com.tts.message.common.CommonStruct.SideCd;
import com.tts.message.market.MarketMarkerStruct.AddMarketAdjustmentLiquidity;
import com.tts.message.market.MarketMarkerStruct.CancelMarketMakingRequest;
import com.tts.message.market.MarketMarkerStruct.MarketMakingStatus;
import com.tts.message.market.FullBookStruct.FullBook;
import com.tts.message.market.FullBookStruct.Tick;
import com.tts.message.util.TtMsgEncoder;
import com.tts.plugin.adapter.feature.ITraderAdjustmentApp;
import com.tts.protocol.platform.IMsgListener;
import com.tts.protocol.platform.IMsgProperties;
import com.tts.protocol.platform.IMsgReceiver;
import com.tts.protocol.platform.IMsgReceiverFactory;
import com.tts.protocol.platform.IMsgSender;
import com.tts.protocol.platform.IMsgSenderFactory;
import com.tts.protocol.platform.IMsgSessionInfo;
import com.tts.protocol.platform.event.IEventMessageTypeConstant;
import com.tts.util.AppContext;
import com.tts.vo.NumberVo;

public class TraderAdjustmentApp implements ITraderAdjustmentApp, IMsgListener {
	private final static Logger LOGGER = LoggerFactory.getLogger(TraderAdjustmentApp.class);
	private final static BankLiquidityAdjustment NO_BANK_LIQUIDITY_ADJUSTMENT = new BankLiquidityAdjustment(0L, 0, -1L, null);

	private final ConcurrentHashMap<String, BankLiquidityAdjustment> instrumentBidPosition;
	private final ConcurrentHashMap<String, BankLiquidityAdjustment> instrumentAskPosition;

	private final IMsgSender msgSender;
	private final IMsgReceiver msgReceiver;

	public TraderAdjustmentApp(String[] symbols) {
		this.instrumentBidPosition = new ConcurrentHashMap<String, BankLiquidityAdjustment>();
		this.instrumentAskPosition = new ConcurrentHashMap<String, BankLiquidityAdjustment>();

		for (int i = 0; i < symbols.length; i++) {
			this.instrumentBidPosition.put(symbols[i], NO_BANK_LIQUIDITY_ADJUSTMENT);
			this.instrumentAskPosition.put(symbols[i], NO_BANK_LIQUIDITY_ADJUSTMENT);
		}

		IMsgSenderFactory msgSenderFactory = AppContext.getContext().getBean(IMsgSenderFactory.class);
		IMsgSender msgSender = msgSenderFactory.getMsgSender(true, false, false);
		msgSender.init();

		IMsgReceiverFactory msgReceiverFactory = AppContext.getContext().getBean(IMsgReceiverFactory.class);
		IMsgReceiver msgReceiver = msgReceiverFactory.getMsgReceiver(false, false, false);
		msgReceiver.setListener(this);
		msgReceiver.setTopic(IEventMessageTypeConstant.MarketMaking.ALL_MARKET_ADJUSTMENT_REQUEST);
		msgReceiver.start();
		this.msgSender = msgSender;
		this.msgReceiver = msgReceiver;
	}

	public void destroy() {
		this.msgReceiver.destroy();
	}

	@Override
	public void adjustPriceRequest(final String symbol, final SideCd side, final double bankWantedSize,
			final double delta, final double limitPrice, final OwnerInfo ownerInfo) {

		if (side == SideCd.BID) {
			instrumentBidPosition.compute(symbol, new BiFunction<String, BankLiquidityAdjustment, BankLiquidityAdjustment>() {

				@Override
				public BankLiquidityAdjustment apply(String t, BankLiquidityAdjustment u) {
					double newPosition = u.getSize() + bankWantedSize;
					if (newPosition < 0) {
						LOGGER.info(String.format("New position for %s on BID below zero. Resetting... ", symbol));
						return NO_BANK_LIQUIDITY_ADJUSTMENT;
					}
					LOGGER.info(String.format("New position for %s on BID = ", symbol) + formatSize(newPosition));
					return new BankLiquidityAdjustment(newPosition, delta, limitPrice, ownerInfo);
				}

			});
		}
		if (side == SideCd.ASK) {
			instrumentAskPosition.compute(symbol, new BiFunction<String, BankLiquidityAdjustment, BankLiquidityAdjustment>() {

				@Override
				public BankLiquidityAdjustment apply(String t, BankLiquidityAdjustment u) {
					double newPosition = u.getSize() + bankWantedSize;
					if (newPosition < 0) {
						LOGGER.info(String.format("New position for %s on ASK below zero. Resetting... ", symbol));
						return NO_BANK_LIQUIDITY_ADJUSTMENT;
					}
					LOGGER.info(String.format("New position for %s on ASK = ", symbol) + formatSize(newPosition));
					return new BankLiquidityAdjustment(newPosition, delta, limitPrice, ownerInfo);
				}

			});
		}
	}

	@Override
	public CoveringResult coverAdjustment(final String symbol, final BuySellActionCd clientDirection, final double ccy1size) {
		final CoveringResult coverResult = new CoveringResult();
		if (clientDirection == BuySellActionCd.SELL) {
			instrumentBidPosition.compute(symbol, new BiFunction<String, BankLiquidityAdjustment, BankLiquidityAdjustment>() {

				@Override
				public BankLiquidityAdjustment apply(String t, BankLiquidityAdjustment u) {
					if (u == NO_BANK_LIQUIDITY_ADJUSTMENT || u.getSize() < ccy1size) {
						coverResult.setCoveredAmount(0);
						return NO_BANK_LIQUIDITY_ADJUSTMENT;
					}
					double newPosition = u.getSize() - ccy1size;
					coverResult.setCoveredAmount(ccy1size);
					coverResult.setProvider(u.getOwnerInfo());
					if (newPosition <= 0) {
						LOGGER.info(String.format("New position for %s on BID below zero. Resetting... ", symbol));
						sendBankLiquidityClearNotification(BuySellActionCd.BUY, symbol);
						return NO_BANK_LIQUIDITY_ADJUSTMENT;
					}
					LOGGER.info(String.format("New position for %s on BID = ", symbol) + formatSize(newPosition));
					return new BankLiquidityAdjustment(newPosition, u.getAdjustment(), u.getLimitPrice(), u.getOwnerInfo());
				}

			});
		}
		if (clientDirection == BuySellActionCd.BUY) {
			instrumentAskPosition.compute(symbol, new BiFunction<String, BankLiquidityAdjustment, BankLiquidityAdjustment>() {

				@Override
				public BankLiquidityAdjustment apply(String t, BankLiquidityAdjustment u) {
					if (u == NO_BANK_LIQUIDITY_ADJUSTMENT|| u.getSize() < ccy1size) {
						coverResult.setCoveredAmount(0);
						return u;
					}
					double newPosition = u.getSize() - ccy1size;
					coverResult.setCoveredAmount(ccy1size);
					coverResult.setProvider(u.getOwnerInfo());
					if (newPosition <= 0) {
						LOGGER.info(String.format("New position for %s on ASK below zero. Resetting... ", symbol));
						sendBankLiquidityClearNotification(BuySellActionCd.SELL, symbol);
						return NO_BANK_LIQUIDITY_ADJUSTMENT;
					}
					LOGGER.info(String.format("New position for %s on ASK = ", symbol) + formatSize(newPosition));
					return new BankLiquidityAdjustment(newPosition, u.getAdjustment(), u.getLimitPrice(), u.getOwnerInfo());
				}

			});
		}
		return coverResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.tts.mas.feature.position.ITraderAdjustmentApp#adjustBook(java.lang.
	 * String, com.tts.message.market.FullBookStruct.FullBook.Builder)
	 */
	@Override
	public FullBook.Builder adjustBook(final String symbol, final FullBook.Builder fbBuilder) {
		if (fbBuilder.getBidTicksCount() == 0 || fbBuilder.getAskTicksCount() == 0) {
			return fbBuilder;
		}
		boolean active = false;
		BankLiquidityAdjustment bidAdjustment = instrumentBidPosition.get(symbol);
		BankLiquidityAdjustment askAdjustment = instrumentAskPosition.get(symbol);

		NumberVo tobBidRate = NumberVo.getInstance(fbBuilder.getBidTicks(fbBuilder.getTopOfBookIdx()).getRate());
		NumberVo tobAskRate = NumberVo.getInstance(fbBuilder.getAskTicks(fbBuilder.getTopOfBookIdx()).getRate());
		NumberVo bidLimitPrice = NumberVo.fromBigDecimal(BigDecimal.valueOf(bidAdjustment.limitPrice));
		NumberVo askLimitPrice = NumberVo.fromBigDecimal(BigDecimal.valueOf(askAdjustment.limitPrice));
		NumberVo bidBoundaryPrice = tobAskRate.isGreater(bidLimitPrice) ? bidLimitPrice : tobAskRate;
		NumberVo askBoundaryPrice = tobAskRate.isLess(askLimitPrice) ? askLimitPrice : tobBidRate;

		if (bidAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT && tobBidRate.isLess(bidLimitPrice)) {
			int tickCount = fbBuilder.getBidTicksCount();
			for (int i = 0; i < tickCount; i++) {
				Tick.Builder t = fbBuilder.getBidTicksBuilder(i);
				if (t.getSize() <= bidAdjustment.getSize()) {
					NumberVo newValue = NumberVo.getInstance(t.getRate()).plus(bidAdjustment.getAdjustment());
					if (newValue.isGreater(bidBoundaryPrice)) {
						newValue = bidBoundaryPrice;
					}
					String newValueStr = newValue.getValue();
					t.setRate(newValueStr);
					t.setSpotRate(newValueStr);
				}
			}
			active = true;
		}

		if (askAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT && tobAskRate.isGreater(askLimitPrice)) {
			int tickCount = fbBuilder.getAskTicksCount();
			for (int i = 0; i < tickCount; i++) {
				Tick.Builder t = fbBuilder.getAskTicksBuilder(i);
				if (t.getSize() <= askAdjustment.getSize()) {
					NumberVo newValue = NumberVo.getInstance(t.getRate()).plus(askAdjustment.getAdjustment());
					if (newValue.isLess(askBoundaryPrice)) {
						newValue = askBoundaryPrice;
					}
					String newValueStr = newValue.getValue();
					t.setRate(newValueStr);
					t.setSpotRate(newValueStr);
				}
			}
			active = true;
		}

		if (bidAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT) {
			MarketMakingStatus.Builder _marketMakingStatus = MarketMakingStatus.newBuilder();
			_marketMakingStatus.setSymbol(symbol);
			_marketMakingStatus.setAdjustment(formatRate(Math.abs(bidAdjustment.getAdjustment())));
			_marketMakingStatus.setSize(formatSize(bidAdjustment.getSize()));
			_marketMakingStatus.setLimitPrice(formatRate(bidAdjustment.getLimitPrice()));
			_marketMakingStatus.setBuySellActionCd(BuySellActionCd.BUY);
			if ( active ) {
				_marketMakingStatus.setActiveFlag(1);
			} else {
				_marketMakingStatus.setActiveFlag(0);
			}
			if ( bidAdjustment.getOwnerInfo() != null ) {
				_marketMakingStatus.setOwner(bidAdjustment.getOwnerInfo().getOwnerName());
				_marketMakingStatus.setOwnerIntCustId(bidAdjustment.getOwnerInfo().getOwnerIntCustId());
				_marketMakingStatus.setOwnerIntAcctId(bidAdjustment.getOwnerInfo().getOwnerIntAcctId());
			}
			MarketMakingStatus marketMakingStatus = _marketMakingStatus.build();
			TtMsg ttMsg = TtMsgEncoder.encode(marketMakingStatus);
			String topic = String.format(IEventMessageTypeConstant.MarketMaking.MARKET_MAKING_STATUS_TEMPLATE, fbBuilder.getSymbol(), BuySellActionCd.BUY.toString());
			msgSender.send(topic, ttMsg);
		}
		if (askAdjustment != NO_BANK_LIQUIDITY_ADJUSTMENT) {
			MarketMakingStatus.Builder _marketMakingStatus = MarketMakingStatus.newBuilder();
			_marketMakingStatus.setSymbol(symbol);
			_marketMakingStatus.setAdjustment(formatRate(Math.abs(askAdjustment.getAdjustment())));
			_marketMakingStatus.setSize(formatSize(askAdjustment.getSize()));
			_marketMakingStatus.setLimitPrice(formatRate(askAdjustment.getLimitPrice()));
			_marketMakingStatus.setBuySellActionCd(BuySellActionCd.SELL);
			if ( active ) {
				_marketMakingStatus.setActiveFlag(1);
			} else {
				_marketMakingStatus.setActiveFlag(0);
			}
			if ( askAdjustment.getOwnerInfo() != null ) {
				_marketMakingStatus.setOwner(askAdjustment.getOwnerInfo().getOwnerName());
				_marketMakingStatus.setOwnerIntCustId(askAdjustment.getOwnerInfo().getOwnerIntCustId());
				_marketMakingStatus.setOwnerIntAcctId(askAdjustment.getOwnerInfo().getOwnerIntAcctId());
			}
			MarketMakingStatus marketMakingStatus = _marketMakingStatus.build();
			TtMsg ttMsg = TtMsgEncoder.encode(marketMakingStatus);
			String topic = String.format(IEventMessageTypeConstant.MarketMaking.MARKET_MAKING_STATUS_TEMPLATE, fbBuilder.getSymbol(), BuySellActionCd.SELL.toString());
			msgSender.send(topic, ttMsg);
		}

		return fbBuilder;
	}

	private static class BankLiquidityAdjustment {
		private final double adjustSize;
		private final double adjustment;
		private final double limitPrice;
		private final OwnerInfo ownerInfo;

		public BankLiquidityAdjustment(double size, double adjustment, double limitPrice, OwnerInfo ownerInfo) {
			super();
			this.adjustSize = size;
			this.adjustment = adjustment;
			this.limitPrice = limitPrice;
			this.ownerInfo = ownerInfo;
		}

		public double getSize() {
			return adjustSize;
		}

		public double getAdjustment() {
			return adjustment;
		}

		public double getLimitPrice() {
			return limitPrice;
		}

		public OwnerInfo getOwnerInfo() {
			return ownerInfo;
		}

	}

	@Override
	public void onMessage(TtMsg arg0, IMsgSessionInfo arg1, IMsgProperties arg2) {
		try {
			String originatingTopic = arg2.getSendTopic();
			if (IEventMessageTypeConstant.MarketMaking.MARKET_ADJUSTMENT_SUBMIT_REQUEST.equals(originatingTopic)) {
				AddMarketAdjustmentLiquidity adjustmentLqy = AddMarketAdjustmentLiquidity
						.parseFrom(arg0.getParameters());
				OwnerInfo ownerInfo = null;

				if (adjustmentLqy.hasOwner() && adjustmentLqy.hasOwnerIntAcctId()
						&& adjustmentLqy.hasOwnerIntCustId()) {
					ownerInfo = new OwnerInfo(adjustmentLqy.getOwner(), adjustmentLqy.getOwnerIntCustId(),
							adjustmentLqy.getOwnerIntAcctId());
				}
				double adjustment = adjustmentLqy.getAdjustment();
				SideCd side = adjustmentLqy.getSide();
				if ( side == SideCd.ASK) {
					adjustment = -1.0 * adjustment;
				}
				this.adjustPriceRequest(adjustmentLqy.getSymbol(), side, adjustmentLqy.getSize(),
						adjustment, adjustmentLqy.getLimitPrice(), ownerInfo);
			} else if (IEventMessageTypeConstant.MarketMaking.MARKET_ADJUSTMENT_CANCEL_REQUEST.equals(originatingTopic)) {
				CancelMarketMakingRequest request = CancelMarketMakingRequest.parseFrom(arg0.getParameters());
				BuySellActionCd buySellActionCd = request.getBuySellActionCd();
				String symbol = request.getSymbol();
				if ( buySellActionCd == BuySellActionCd.BUY) {
					instrumentBidPosition.put(symbol, NO_BANK_LIQUIDITY_ADJUSTMENT);
				} else if ( buySellActionCd == BuySellActionCd.SELL) {
					instrumentAskPosition.put(symbol, NO_BANK_LIQUIDITY_ADJUSTMENT);
				} 
				sendBankLiquidityClearNotification(buySellActionCd, symbol);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void sendBankLiquidityClearNotification(BuySellActionCd buySellActionCd, String symbol) {
		MarketMakingStatus.Builder _marketMakingStatus = MarketMakingStatus.newBuilder();
		_marketMakingStatus.setSymbol(symbol);
		_marketMakingStatus.setAdjustment(formatRate(0d));
		_marketMakingStatus.setSize(formatSize(0d));
		_marketMakingStatus.setLimitPrice(formatRate(0d));
		_marketMakingStatus.setBuySellActionCd(buySellActionCd);
		_marketMakingStatus.setActiveFlag(0);
		MarketMakingStatus marketMakingStatus = _marketMakingStatus.build();
		TtMsg ttMsg = TtMsgEncoder.encode(marketMakingStatus);
		String topic = String.format(IEventMessageTypeConstant.MarketMaking.MARKET_MAKING_STATUS_TEMPLATE, symbol, buySellActionCd.toString());
		msgSender.send(topic, ttMsg);
	}

	public static String formatSize(double d) {
		DecimalFormat df = new DecimalFormat("0.00");
		return df.format(d);
	}

	public static String formatRate(double d) {
		DecimalFormat df = new DecimalFormat("0.00000");
		return df.format(d);
	}

}
