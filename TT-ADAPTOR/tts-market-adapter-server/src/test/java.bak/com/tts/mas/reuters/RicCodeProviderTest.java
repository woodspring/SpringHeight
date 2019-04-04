package com.tts.mas.reuters;

import com.tts.reuters.adapter.utils.ric.FileBasedRicCodeProvider;
import com.tts.vo.TenorVo;

public class RicCodeProviderTest {
	public static void main(String[] args ) throws InterruptedException {
		FileBasedRicCodeProvider fbrp = new FileBasedRicCodeProvider();
		
		
		String ricCode = fbrp.getRicCode("USDTRY", TenorVo.NOTATION_OVERNIGHT);
		System.out.println(ricCode);
		
		
		Thread.sleep(10000);
		fbrp.reload();
		String ricCode2 = fbrp.getRicCode("USDTRY", TenorVo.NOTATION_OVERNIGHT);
		System.out.println(ricCode2);
	}
}
