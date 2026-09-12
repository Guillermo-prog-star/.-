package com.integrityfamily.familyhome.application.exception;

import com.integrityfamily.dto.home.JourneyStage;

public class UnsupportedJourneyStageException extends FamilyHomeProjectionException {
    public UnsupportedJourneyStageException(JourneyStage stage) {
        super("Unsupported journey stage: " + stage);
    }
}
