package com.tts.ske.vo;

import java.lang.reflect.Method;

import com.tts.message.trade.RestingOrderMessage.OrderParams;
import com.tts.message.trade.RestingOrderMessage.OrderParamsOrBuilder;
import com.tts.vo.IOrderParams;

public class OrderParamsVo implements IOrderParams {

	private OrderType orderType = null;
	private String targetPrice;
	private String quoteRefId;
	private TimeInForceType timeInForce;
	
	@Override
	public OrderType getOrdType() {
		return orderType;
	}

	@Override
	public String getTargetPrice() {
		return targetPrice;
	}

	@Override
	public TimeInForceType getTimeInForce() {
		return timeInForce;
	}

	@Override
	public void setOrdType(OrderType arg0) {
		this.orderType = arg0;		
	}

	@Override
	public void setTargetPrice(String arg0) {
		this.targetPrice = arg0;		
	}

	@Override
	public void setTimeInForce(TimeInForceType arg0) {
		this.timeInForce = arg0	;
	}
	
	public OrderParams toMessage() {
		OrderParams.Builder op = OrderParams.newBuilder();
		if ( this.orderType == OrderType.LIMIT)		op.setOrdType(OrderParams.OrdType.Limit);
		else if ( this.orderType == OrderType.MARKET)		op.setOrdType(OrderParams.OrdType.Market);
		else if ( this.orderType == OrderType.PREVIOUSLY_QUOTED) op.setOrdType(OrderParams.OrdType.Previously_Quoted);
		if ( this.timeInForce == TimeInForceType.FILL_OR_KILL ) op.setTimeInForce(OrderParams.TimeInForce.Fill_or_Kill);
		else if ( this.timeInForce == TimeInForceType.GOOD_TILL_CANCEL ) op.setTimeInForce(OrderParams.TimeInForce.Good_Till_Cancel);
		else if ( this.timeInForce == TimeInForceType.GOOD_TILL_DATE ) op.setTimeInForce(OrderParams.TimeInForce.Good_Till_Date);
		else if ( this.timeInForce == TimeInForceType.IMMEDIATE_OR_CANCEL ) op.setTimeInForce(OrderParams.TimeInForce.Immediate_or_Cancel);
		if (targetPrice != null ) op.setTargetPrice(targetPrice);
		if (quoteRefId != null ) op.setQuoteRefId(getQuoteRefId());
		return op.build();
	}
	
	public static OrderParamsVo fromMessage(OrderParamsOrBuilder op) {
		OrderParamsVo vo = new OrderParamsVo();
		if ( op.hasOrdType() ) {
			if ( op.getOrdType() == OrderParams.OrdType.Limit)		vo.setOrdType(OrderType.LIMIT);
			else if ( op.getOrdType() == OrderParams.OrdType.Market)		vo.setOrdType(OrderType.MARKET);
			else if ( op.getOrdType() == OrderParams.OrdType.Previously_Quoted ) vo.setOrdType(OrderType.PREVIOUSLY_QUOTED);
		}
		if ( op.hasTimeInForce() ) {
			if ( op.getTimeInForce() == OrderParams.TimeInForce.Fill_or_Kill ) vo.timeInForce = TimeInForceType.FILL_OR_KILL;
			else if ( op.getTimeInForce() == OrderParams.TimeInForce.Good_Till_Cancel ) vo.timeInForce = TimeInForceType.GOOD_TILL_CANCEL;
			else if ( op.getTimeInForce() == OrderParams.TimeInForce.Good_Till_Date ) vo.timeInForce = TimeInForceType.GOOD_TILL_DATE;
			else if ( op.getTimeInForce() == OrderParams.TimeInForce.Immediate_or_Cancel ) vo.timeInForce = TimeInForceType.IMMEDIATE_OR_CANCEL;
		}
		if (op.hasTargetPrice() ) vo.setTargetPrice(op.getTargetPrice());
		if (op.hasQuoteRefId() ) vo.setQuoteRefId(op.getQuoteRefId());
		return vo;
	}
	
	public OrderParamsVo deepClone() {
		OrderParamsVo orderParams = new OrderParamsVo();
		orderParams.setOrdType(this.getOrdType());
		orderParams.setTargetPrice(this.getTargetPrice());
		orderParams.setTimeInForce(this.getTimeInForce());
		orderParams.setQuoteRefId(this.getQuoteRefId());
		return orderParams;
	}
	
	public static void main(String[] args ) {
		Method[] methods = OrderParamsVo.class.getMethods();
		for ( Method m: methods) {
			if ( m.getName().startsWith("set") ) {
				String field = m.getName().substring(3);
				System.out.println("t.set" + field + "(this.get" +field + "());");
			}
		}
	}

	public String getQuoteRefId() {
		return quoteRefId;
	}

	public void setQuoteRefId(String quoteRefId) {
		this.quoteRefId = quoteRefId;
	}


}
