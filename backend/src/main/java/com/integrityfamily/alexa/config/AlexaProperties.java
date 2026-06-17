package com.integrityfamily.alexa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.alexa")
public class AlexaProperties {

    private boolean enabled = false;
    private String skillId = "";
    private String clientId = "integrity-family-alexa";
    private long tokenTtlSeconds = 3600;
    private long codeTtlSeconds = 300;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public String getSkillId() { return skillId; }
    public void setSkillId(String v) { this.skillId = v; }
    public String getClientId() { return clientId; }
    public void setClientId(String v) { this.clientId = v; }
    public long getTokenTtlSeconds() { return tokenTtlSeconds; }
    public void setTokenTtlSeconds(long v) { this.tokenTtlSeconds = v; }
    public long getCodeTtlSeconds() { return codeTtlSeconds; }
    public void setCodeTtlSeconds(long v) { this.codeTtlSeconds = v; }
}
