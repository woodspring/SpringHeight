package com.tts.ske.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tts.service.biz.instrument.util.ISymbolIdMapper;
import com.tts.ske.vo.BankLiquidityAdjustmentVo;
import com.tts.util.AppContext;
import com.tts.util.collection.FixedWidthFileStore;;

public class BankLiquidityStore {
	public final static BankLiquidityAdjustmentVo NO_BANK_LIQUIDITY_ADJUSTMENT = new BankLiquidityAdjustmentVo(0L, 0, -1L, null);
	private final static Logger logger = LoggerFactory.getLogger(BankLiquidityStore.class);

	private static final String TAG__ASK = BankLiquidityAdjustmentVo.DELIMITER + "A" + BankLiquidityAdjustmentVo.DELIMITER;

	private static final String TAG__BID = BankLiquidityAdjustmentVo.DELIMITER + "B" + BankLiquidityAdjustmentVo.DELIMITER;
	
	private static final int PREFIX_LENGTH = 6 + TAG__BID.length();

	private static final String BANK_LIQUIDITY_STORE_FILE_NAME = "bankLiquidityStore/bankLiquidity.store";
	private final FixedWidthFileStore store;
	
	private final Map<String, BankLiquidityAdjustmentVo> lqy = new ConcurrentHashMap<>();
	private final ISymbolIdMapper symbolIdMapper;
	
	public BankLiquidityStore() {
		File f = new File(BANK_LIQUIDITY_STORE_FILE_NAME);
		File parentFolder = f.getParentFile();
		
		if ( !parentFolder.exists()) {
			parentFolder.mkdirs();
		}
		
		FixedWidthFileStore _store= null;
		ISymbolIdMapper symbolIdMapper = AppContext.getContext().getBean(ISymbolIdMapper.class);

		try {
			_store = new FixedWidthFileStore(150, BANK_LIQUIDITY_STORE_FILE_NAME);
			List<String> symbols = symbolIdMapper.getSymbols();

			if ( !f.exists()) {
				int i = 0; 
				for ( String symbol : symbols) {
					_store.updateEntry(i++, symbol + TAG__BID + NO_BANK_LIQUIDITY_ADJUSTMENT.objToString());
					_store.updateEntry(i++, symbol + TAG__ASK + NO_BANK_LIQUIDITY_ADJUSTMENT.objToString());
					lqy.put(symbol + TAG__BID, NO_BANK_LIQUIDITY_ADJUSTMENT);
					lqy.put(symbol + TAG__ASK, NO_BANK_LIQUIDITY_ADJUSTMENT);
				}
			} else {
				for ( String symbol : symbols) {
					int symbolId = symbolIdMapper.map(symbol);
					int bidEntryId = symbolId *2;
					int askEntryId = bidEntryId + 1;
					String bidLine = _store.getEntry(bidEntryId);
					String askLine = _store.getEntry(askEntryId);
					if ( bidLine.startsWith(symbol)) {
						bidLine = bidLine.substring(PREFIX_LENGTH);
						BankLiquidityAdjustmentVo adj = BankLiquidityAdjustmentVo.fromString(bidLine);
						if ( adj.getSize() == NO_BANK_LIQUIDITY_ADJUSTMENT.getSize()) {
							lqy.put(symbol + TAG__BID, NO_BANK_LIQUIDITY_ADJUSTMENT);
						} else {
							lqy.put(symbol + TAG__BID, adj);
						}
					} else {
						lqy.put(symbol + TAG__BID, NO_BANK_LIQUIDITY_ADJUSTMENT);
					}
					if ( askLine.startsWith(symbol)) {
						askLine = askLine.substring(PREFIX_LENGTH);
						BankLiquidityAdjustmentVo adj = BankLiquidityAdjustmentVo.fromString(askLine);
						if ( adj.getSize() == NO_BANK_LIQUIDITY_ADJUSTMENT.getSize()) {
							lqy.put(symbol + TAG__ASK, NO_BANK_LIQUIDITY_ADJUSTMENT);
						} else {
							lqy.put(symbol + TAG__ASK, adj);
						}
					} else {
						lqy.put(symbol + TAG__ASK, NO_BANK_LIQUIDITY_ADJUSTMENT);
					}
				}
			}
		} catch (FileNotFoundException e) {
			_store = null;
		}

		this.store = _store;
		this.symbolIdMapper = symbolIdMapper;
	}
	
	public void updateBidLiquidity(String symbol, BankLiquidityAdjustmentVo bidLqyAdjustment) {		
		lqy.put(symbol + TAG__BID, bidLqyAdjustment);
		int symbolId = symbolIdMapper.map(symbol);
		int bidEntryId = symbolId *2;
		String newValue = symbol + TAG__BID + bidLqyAdjustment.objToString();
		this.store.updateEntry(bidEntryId, newValue);
		logger.info("New Value " + newValue);
	}
	
	public void updateAskLiquidity(String symbol, BankLiquidityAdjustmentVo askLqyAdjustment) {
		lqy.put(symbol + TAG__ASK, askLqyAdjustment);
		int symbolId = symbolIdMapper.map(symbol);
		int bidEntryId = symbolId *2;
		int askEntryId = bidEntryId + 1;
		String newValue = symbol + TAG__ASK + askLqyAdjustment.objToString();
		this.store.updateEntry(askEntryId, newValue);
		logger.info("New Value " + newValue);

	}
	
	
	public BankLiquidityAdjustmentVo getBidLiquidity(String symbol) {
		return lqy.get(symbol + TAG__BID);
	}
	
	public BankLiquidityAdjustmentVo getAskLiquidity(String symbol) {
		return lqy.get(symbol + TAG__ASK);
	}
	
}

