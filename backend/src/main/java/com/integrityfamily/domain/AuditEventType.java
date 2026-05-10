// backend/src/main/java/com/integrityfamily/domain/AuditEventType.java
package com.integrityfamily.domain;

public enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    LOGOUT,
    SESSION_EXPIRED,
    FAMILY_REGISTERED,
    CLI_COMMAND_EXECUTED,
    EVALUATION_STARTED,
    EVALUATION_SUBMITTED,
    PLAN_TASK_TOGGLED
}


