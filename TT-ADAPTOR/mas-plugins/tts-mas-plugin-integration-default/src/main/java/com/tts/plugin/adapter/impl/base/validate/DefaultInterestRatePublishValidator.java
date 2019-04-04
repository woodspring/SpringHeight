package com.tts.plugin.adapter.impl.base.validate;

import com.tts.message.market.InterestRateCurveStruct.InterestRateCurve;
import com.tts.plugin.adapter.api.app.validate.IPublishValidator;

public class DefaultInterestRatePublishValidator implements IPublishValidator<InterestRateCurve.Builder> {

	@Override
	public InterestRateCurve.Builder validate(InterestRateCurve.Builder irC) {
		return irC;
	}

}
