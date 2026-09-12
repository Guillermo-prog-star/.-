package com.integrityfamily.familyhome.port;

import java.util.List;

public record AssessmentReturnSnapshot(
    List<String> strengths,
    String priorityOpportunity,
    boolean detailsAvailable
) {
}
