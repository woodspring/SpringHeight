package com.tts.mlp.app.price.data;

import java.util.Random;

public class MarketDirection {
	
	private final Random moveDownRandom;
	private final Random moveUpRandom;
	private final Random smallMoveRandom;

	public MarketDirection() {
		super();
		this.moveDownRandom =  new Random();
		this.moveUpRandom =  new Random();
		this.smallMoveRandom =  new Random();
	}
	
	public int getNextMovement() {
		boolean moveDown = moveDownRandom.nextBoolean();
		boolean moveUp = moveUpRandom.nextBoolean();
		
		if ( !moveDown && moveUp) {
			return 2;
		} else if ( !moveUp && moveDown) {
			return -2;
		}
		boolean smallMove = moveUpRandom.nextBoolean();
		if ( smallMove ) {
			return 1;
		}
		
		return -1;
	}

	public static void main(String[] args) {
		MarketDirection d = new MarketDirection();
		for ( int i = 0; i < 1000 ; i ++) {
			System.out.println(d.getNextMovement());
		}
	}
}
