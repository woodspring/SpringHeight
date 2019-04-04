package com.tts.mas;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.protobuf.TextFormat;
import com.tts.mas.config.MasReutersConfiguration;
import com.tts.message.market.ForwardCurveStruct.ForwardCurve.Builder;
import com.tts.plugin.adapter.support.IReutersApp;
import com.tts.plugin.adapter.support.IReutersRFAMsgListener;
import com.tts.util.config.UtilConfiguration;


public class TRAdapterTest implements IReutersRFAMsgListener {

	public TRAdapterTest()	{
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		ctx.register(UtilConfiguration.class);
		//ctx.register(MessageConfiguration.class);
		ctx.register(MasReutersConfiguration.class);
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		ctx.registerShutdownHook();
		
		IReutersApp rfaAdapter = ctx.getBean(IReutersApp.class);
		rfaAdapter.beginReutersApp();
		
		rfaAdapter.subscribeToFwdRIC("EURUSD", "ON", this);
		rfaAdapter.subscribeToFwdRIC("EURUSD", "TN", this);
		rfaAdapter.subscribeToFwdRIC("USDTRY", "ON", this);
		rfaAdapter.subscribeToFwdRIC("USDTRY", "TN", this);
		
		try	{
			Thread.sleep(12000L);
		}
		catch(InterruptedException exp)	{
			exp.printStackTrace();
		}
		
		rfaAdapter.unsubscribeFromFwdRIC();
		rfaAdapter.endReutersApp();
		
		ctx.close();
	}
	public static void main(String[] args) {
		new TRAdapterTest();
	}

	@Override
	public void onFwdRICMessage(String SubscriptionID, Builder fwdPrice) {
		System.out.println(">>>" + SubscriptionID + " <<>> " + TextFormat.shortDebugString(fwdPrice));
	}
}