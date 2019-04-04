package com.tts.mlp.app.price.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.tts.mlp.app.ForwardCurveDataManager;
import com.tts.mlp.app.GlobalAppConfig;
import com.tts.mlp.app.price.data.IRandomMarketPriceProvider;
import com.tts.mlp.rate.provider.vo.Instrument;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo;
import com.tts.mlp.rate.provider.vo.SubscriptionRequestVo.QuoteSide;
import com.tts.mlp.rate.provider.vo.Tick;
import com.tts.util.chronology.ChronologyUtil;
import com.tts.util.collection.formatter.DoubleFormatter;
import com.tts.vo.TenorVo;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.BidForwardPoints;
import quickfix.field.BidForwardPoints2;
import quickfix.field.BidPx;
import quickfix.field.BidSize;
import quickfix.field.BidSpotRate;
import quickfix.field.OfferForwardPoints;
import quickfix.field.OfferForwardPoints2;
import quickfix.field.OfferPx;
import quickfix.field.OfferSize;
import quickfix.field.OfferSpotRate;
import quickfix.field.OnBehalfOfCompID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.QuoteCancelType;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.field.QuoteResponseLevel;
import quickfix.field.SecurityType;
import quickfix.field.SettlDate;
import quickfix.field.SettlDate2;
import quickfix.field.SettlType;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;

public class RfsHandler extends AbstractSubscriptionHandler {
	
	private final String clientReqId;
	private final long endTime;
	private final IRandomMarketPriceProvider priceProvider;
	private final SubscriptionRequestVo request;
	private double[] swapPointsNear;
	private double[] swapPointsFar; 
	
	public RfsHandler(SubscriptionRequestVo request, IRandomMarketPriceProvider priceProvider, SessionID sessionid, quickfix.Message originalMessage) {
		super(request, sessionid, originalMessage);
		this.priceProvider = priceProvider;
		this.request = request;
		this.clientReqId = request.getClientReqId();
		
		boolean sizeNotOk = validateQuoteSize(request.getSymbol(), request.getSize());
		if ( !sizeNotOk ) {
			this.endTime = 1;
		} else if ( request.getExpiryTime() > 0 ) {
			this.endTime = System.currentTimeMillis() + request.getExpiryTime() * ChronologyUtil.MILLIS_IN_SECOND;
		} else {
			this.endTime = -1;
		}
		System.out.println("endTime = " + this.endTime);
	}
	

	private boolean validateQuoteSize(String symbol, long size) {
		Instrument instrument = priceProvider.getCurrentPrice(symbol);
		List<Tick> ticks = instrument.getBidTicks();
		long maxRung = -1L;
		for ( Tick tick: ticks) {
			if ( tick.getQuantity().longValue() > maxRung) {
				maxRung = tick.getQuantity().longValue();
			}
		}
		if ( size > maxRung) {
			return false;
		}
		return true;
	}


	@Override
	public Message push(long seqQuoteId) {
		if ( endTime > 0 && System.currentTimeMillis() > endTime ) {
			quickfix.fix50.QuoteCancel quoteCancel = new quickfix.fix50.QuoteCancel();
			quoteCancel.set(new QuoteReqID(clientReqId));
			quoteCancel.set(new QuoteID(getIdentity() + "!" + seqQuoteId));
			quoteCancel.set(new QuoteCancelType(QuoteCancelType.CANCEL_QUOTE_SPECIFIED_IN_QUOTEID));
			getSession().send(quoteCancel);
			return quoteCancel; 
		}
		final Instrument instrument = GlobalAppConfig.isRateFreezed() ? priceProvider.getCurrentPrice(request.getSymbol()) : priceProvider.getNextMarketPrice(request.getSymbol());
		if ( instrument.getAskTicks().size() == 1 && instrument.getAskTicks().get(0).getQuantity() == 0.0d) {
			quickfix.fix50.QuoteCancel quoteCancel = new quickfix.fix50.QuoteCancel();
			quoteCancel.set(new QuoteReqID(clientReqId));
			quoteCancel.set(new QuoteID(getIdentity() + "!" + seqQuoteId));
			quoteCancel.set(new QuoteCancelType(QuoteCancelType.CANCEL_QUOTE_SPECIFIED_IN_QUOTEID));
			getSession().send(quoteCancel);
			return quoteCancel; 
		}
		quickfix.fix50.Quote quote = null;
		if ( "FXSWAP".equals(request.getSecurityType() ) ) {
			quote = buildPricesForSWAP(request, instrument);
		} else {
			quote = buildPricesForSpotFwd(request, instrument);
		}
		
		if ( quote != null ) {
			quote.set(new QuoteID(getIdentity() + "!" + seqQuoteId));
			if ( request.getTenor() != null ) {
				if(request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL) || request.getTenor().equals("SP")) {
					quote.set(new SettlType(SettlType.REGULAR));
				} else {
					quote.set(new SettlType(request.getTenor()));
				}
			}
			if ( request.getOnBehaveOf() != null ) {
				quote.getHeader().setField(new OnBehalfOfCompID(request.getOnBehaveOf()));
			}
			quote.set(new TransactTime());
			getSession().send(quote);
			System.out.println(quote);

		}
		return quote;
	}
	
	private quickfix.fix50.Quote buildPricesForSWAP(SubscriptionRequestVo request, Instrument instrument  ) {
		
		if ( instrument == null ) { return null; }
		String symbol = request.getSymbol();
		String notionalCurrency = request.getNotionalCurrency();
		long size = request.getSize();
		long sizeFar = request.getSizeFar();
		String settleDate = request.getSettleDate();
		String settleDateFar = request.getSettleDateFar();

		if ("USDCAD".equals(symbol) && "CAD".equals(notionalCurrency)
				&& request.getQuoteSide() == QuoteSide.BUY
				&& 500000 == size && 150000 == sizeFar
				&& "20180212".equals(settleDate) && "20180220".equals(settleDateFar)) {
			DataDictionary dd = null;
			quickfix.fix50.Quote message = null;
			try {
				dd = new DataDictionary("app-resources/CIBCFIX50.xml");
				dd.setCheckUnorderedGroupFields(true);
				message = new quickfix.fix50.Quote();
				message.fromString(
						"35=S34=44935115=TICKTRADE128=TICKTRADE131=USDCAD.SPOT.806040000000004231117=JDEYGOJX458053IMSIT1::5301=055=USDCAD167=FXSWAP64=20180209193=20180216134=500000135=50000015=CAD132=1.15873133=1.25899188=1.15873190=1.25899189=0191=06050=1.2581816051=1.1594436052=1500006053=150000642=-0.000809643=0.00071340=G",
						dd, false);
				message.set(new SettlDate(settleDate));
				message.set(new SettlDate2(settleDateFar));
				message.set(new QuoteReqID(request.getClientReqId()));
//				message.getHeader().setString(49, "BANK_ESP_PRICES");;
//				message.getHeader().setString(56, "TTS_ESP_PRICES");;
//				message.setString(49, "BANK_ESP_PRICES");;
//				message.setString(56, "TTS_ESP_PRICES");;
//				
			} catch (ConfigError | InvalidMessage e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return (quickfix.fix50.Quote) message;
		}
		
		int tickIdx = findTickIndex(instrument, size);
		int tickIdxFar = findTickIndex(instrument, sizeFar);

		quickfix.fix50.Quote quote = new quickfix.fix50.Quote();
		quote.set(new QuoteReqID(request.getClientReqId()));

		
		quote.set(new Symbol(symbol));
		quote.set(new OrderQty(size));
		quote.set(new SecurityType("FXSWAP"));
		
		quote.set(new SettlDate(settleDate));
		quote.set(new SettlDate2(settleDateFar));
		
		quote.set(new QuoteResponseLevel(0));
		
		double bidPts = -999999999.0d, offerPts = -999999999.0d;
		double bidPtsFar = -999999999.0d, offerPtsFar = -999999999.0d;

		double[] swapPointsDataNear = getSwapPointsDataNear(request, 0.0);
		double[] swapPointsDataFar = getSwapPointsDataFar(request, 0.05d);

		if ( swapPointsDataNear != null ) {
			bidPts = swapPointsDataNear[0];
			offerPts = swapPointsDataNear[1];
		}
		if ( swapPointsDataFar != null ) {
			bidPtsFar = swapPointsDataFar[0];
			offerPtsFar = swapPointsDataFar[1];
		}
		BigDecimal offerPtsBd = new BigDecimal(offerPts).movePointLeft(instrument.getPointValue()).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP);
		BigDecimal bidPtsBd = new BigDecimal(bidPts).movePointLeft(instrument.getPointValue()).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP);

		Tick offerTick = request.getQuoteSide() == QuoteSide.SELL ? instrument.getAskTicks().get(tickIdxFar) : instrument.getAskTicks().get(tickIdx);
		quote.set(new OfferPx(new BigDecimal(offerTick.getPrice()).add(offerPtsBd).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP).doubleValue()));
		quote.set(new OfferSpotRate(offerTick.getPrice()));
		quote.set(new OfferSize(size));

		quote.set(new OfferForwardPoints(offerPtsBd.doubleValue()));

		Tick bidTick = request.getQuoteSide() == QuoteSide.BUY ? instrument.getBidTicks().get(tickIdxFar) : instrument.getBidTicks().get(tickIdx);
		quote.set(new BidPx(new BigDecimal(bidTick.getPrice()).add(bidPtsBd).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP).doubleValue()));
		quote.set(new BidSpotRate(bidTick.getPrice()));
		quote.set(new BidSize(size));
		quote.set(new BidForwardPoints(bidPtsBd.doubleValue()));

		BigDecimal offerPtsFarBd = new BigDecimal(offerPtsFar).movePointLeft(instrument.getPointValue()).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP);
		BigDecimal bidPtsFarBd = new BigDecimal(bidPtsFar).movePointLeft(instrument.getPointValue()).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP);

		quote.setDouble(6050, new BigDecimal(offerTick.getPrice()).add(bidPtsFarBd).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP).doubleValue());
		quote.setDouble(6051, new BigDecimal(bidTick.getPrice()).add(offerPtsFarBd).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP).doubleValue());
		quote.setString(6052, sizeFar + "");
		quote.setString(6053, sizeFar + "");
		quote.set(new BidForwardPoints2(bidPtsFarBd.doubleValue()));
		quote.set(new OfferForwardPoints2(offerPtsFarBd.doubleValue()));
		quote.set(new OrdType(OrdType.FOREX_SWAP));
		return quote;
	}

	
	private quickfix.fix50.Quote buildPricesForSpotFwd(SubscriptionRequestVo request, Instrument instrument  ) {
		
		if ( instrument == null ) { return null; }
			int tickIdx = findTickIndex(instrument, request.getSize());
	
			quickfix.fix50.Quote quote = new quickfix.fix50.Quote();
			quote.set(new QuoteReqID(request.getClientReqId()));
	
			quote.set(new Symbol(request.getSymbol()));
			quote.set(new OrderQty(request.getSize()));
			
			if(request.getTenor() != null && request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL)) {
				quote.set(new SecurityType("FXSPOT"));
			} else {
				quote.set(new SecurityType("FXFWD"));
			}
			if(request.getSettleDate() != null ) {
				quote.set(new SettlDate(request.getSettleDate()));
			}
			quote.set(new QuoteResponseLevel(0));
			
			if ( request.getQuoteSide() == QuoteSide.BUY) {
				quote.set(new Side(Side.BUY));
			} else if ( request.getQuoteSide() == QuoteSide.SELL) {
				quote.set(new Side(Side.SELL));
			} else {
				quote.set(new Side('0'));
			}
			
			if(request.getTenor() != null && (request.getTenor().equals(TenorVo.NOTATION_SPOT_FULL) || request.getTenor().equals("SP") || request.getTenor().equals("0"))) {

				Tick offerTick = instrument.getAskTicks().get(tickIdx);
				quote.set(new OfferPx(DoubleFormatter.roundDouble(offerTick.getPrice(), instrument.getPrecision(), RoundingMode.HALF_EVEN)));
				quote.set(new OfferSpotRate(DoubleFormatter.roundDouble(offerTick.getPrice(), instrument.getPrecision(), RoundingMode.HALF_EVEN)));
				quote.set(new OfferSize(request.getSize()));


				Tick bidTick = instrument.getBidTicks().get(tickIdx);
				quote.set(new BidPx(DoubleFormatter.roundDouble(bidTick.getPrice(), instrument.getPrecision(), RoundingMode.HALF_EVEN)));

				quote.set(new BidSpotRate(DoubleFormatter.roundDouble(bidTick.getPrice(), instrument.getPrecision(), RoundingMode.HALF_EVEN)));
				quote.set(new BidSize(request.getSize()));

			} else {
				double bidPts = -999999999.0d, offerPts = -999999999.0d;
				double[] swapPointsData = getSwapPointsDataNear(request, 0.05d);
				if ( swapPointsData != null ) {
					bidPts = swapPointsData[0];
					offerPts = swapPointsData[1];
				}
				System.out.println("offerPts=" + offerPts);
				BigDecimal offerPtsBd = new BigDecimal(offerPts).movePointLeft(instrument.getPointValue()).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP);
				BigDecimal bidPtsBd = new BigDecimal(bidPts).movePointLeft(instrument.getPointValue()).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP);
				System.out.println("offerPtsBd.1=" + offerPtsBd.doubleValue());

				Tick offerTick = instrument.getAskTicks().get(tickIdx);
				quote.set(new OfferPx(new BigDecimal(offerTick.getPrice()).add(offerPtsBd).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP).doubleValue()));
				quote.set(new OfferSpotRate(offerTick.getPrice()));
				quote.set(new OfferSize(request.getSize()));
				System.out.println("offerPtsBd.2=" + offerPtsBd.doubleValue());

				quote.set(new OfferForwardPoints(offerPtsBd.doubleValue()));

				Tick bidTick = instrument.getBidTicks().get(tickIdx);
				quote.set(new BidPx(new BigDecimal(bidTick.getPrice()).add(bidPtsBd).setScale(instrument.getPointValue()+2, BigDecimal.ROUND_HALF_UP).doubleValue()));
				quote.set(new BidSpotRate(bidTick.getPrice()));
				quote.set(new BidSize(request.getSize()));
				quote.set(new BidForwardPoints(bidPtsBd.doubleValue()));

			}
		
		
		return quote;
	}

	public double[] getSwapPointsDataNear(SubscriptionRequestVo request, double spread) {
		if ( this.swapPointsNear != null ) {
			return this.swapPointsNear;
		}
		double[] swapPoints = null;
		if ( request.getTenor() == null && request.getSettleDate() != null   && !request.getSettleDate().isEmpty()) {
			swapPoints = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),ChronologyUtil.getLocalDateFromString(request.getSettleDate()));
		} else {
			swapPoints = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),request.getTenor());
			if (swapPoints == null &&  request.getSettleDate() != null   && !request.getSettleDate().isEmpty()) {
				swapPoints = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),ChronologyUtil.getLocalDateFromString(request.getSettleDate()));
			}
			if ( "ON".equals(request.getTenor()) ) {
				double[] swapPointsTN = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),request.getTenor());
				double bid = -1* swapPoints[1] + -1 * swapPointsTN[1];
				double offer = -1* swapPoints[0] + -1 * swapPointsTN[0];
				swapPoints = new double[] { bid, offer};
			} else if ( "TN".equals(request.getTenor()) ) {
				swapPoints = new double[] { swapPoints[1], swapPoints[0]};
			}
		}
		this.swapPointsNear = new double[] { swapPoints[0] * (1.0 - spread), swapPoints[1] * (1.0 + spread) } ;
		System.out.println(String.format("this.swapPointsNear: bidSwapPoints=%s, askSwapPoints=%s", this.swapPointsNear[0], this.swapPointsNear[1]));
		return this.swapPointsNear;
	}
	
	public double[] getSwapPointsDataFar(SubscriptionRequestVo request, double spread) {
		if ( this.swapPointsFar != null ) {
			return this.swapPointsFar;
		}
		double[] swapPoints = null;
		if ( request.getTenor() == null && request.getSettleDateFar() != null   && !request.getSettleDateFar().isEmpty()) {
			swapPoints = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),ChronologyUtil.getLocalDateFromString(request.getSettleDateFar()));
		} else {
			swapPoints = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),request.getTenor());
			if (swapPoints == null &&  request.getSettleDateFar() != null   && !request.getSettleDateFar().isEmpty()) {
				swapPoints = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),ChronologyUtil.getLocalDateFromString(request.getSettleDateFar()));
			}
			if ( "ON".equals(request.getTenor()) ) {
				double[] swapPointsTN = ForwardCurveDataManager.getSwapPoints(request.getSymbol(),request.getTenor());
				double bid = -1* swapPoints[1] + -1 * swapPointsTN[1];
				double offer = -1* swapPoints[0] + -1 * swapPointsTN[0];
				swapPoints = new double[] { bid, offer};
			} else if ( "TN".equals(request.getTenor()) ) {
				swapPoints = new double[] { swapPoints[1], swapPoints[0]};
			}
		}
		this.swapPointsFar = new double[] { swapPoints[0] * (1.0 - spread), swapPoints[1] * (1.0 + spread) } ;
		System.out.println(String.format("this.swapPointsFar: bidSwapPoints=%s, askSwapPoints=%s", this.swapPointsNear[0], this.swapPointsNear[1]));
		return this.swapPointsFar;
	}
	
	private int findTickIndex(Instrument instrument, long size) {
		List<Tick> ticks = instrument.getAskTicks();
		
		for ( int i = 0; i < ticks.size(); i++) {
			Tick tick = ticks.get(i);
			if ( tick.getQuantity() >= size   ) {
				return i;
			}
		}
		return 0;
	}

}
