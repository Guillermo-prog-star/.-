package com.integrityfamily.familyhome.policy;

import com.integrityfamily.dto.home.ConsentStatus;

public class MediaPresentationPolicy {
    public static boolean shouldShowMedia(ConsentStatus consentStatus) {
        return consentStatus == ConsentStatus.GRANTED || consentStatus == ConsentStatus.NOT_REQUIRED;
    }
}
