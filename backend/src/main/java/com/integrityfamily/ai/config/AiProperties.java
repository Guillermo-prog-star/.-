package com.integrityfamily.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private Voice voice = new Voice();
    private Anthropic anthropic = new Anthropic();
    private Openai openai = new Openai();
    private Elevenlabs elevenlabs = new Elevenlabs();

    public Voice getVoice() { return voice; }
    public void setVoice(Voice v) { this.voice = v; }
    public Anthropic getAnthropic() { return anthropic; }
    public void setAnthropic(Anthropic v) { this.anthropic = v; }
    public Openai getOpenai() { return openai; }
    public void setOpenai(Openai v) { this.openai = v; }
    public Elevenlabs getElevenlabs() { return elevenlabs; }
    public void setElevenlabs(Elevenlabs v) { this.elevenlabs = v; }

    public static class Voice {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
    }

    public static class Anthropic {
        private boolean enabled = true;
        private String apiKey = "";
        private String model = "claude-3-5-haiku-20241022";
        private String baseUrl = "https://api.anthropic.com/v1";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
    }

    public static class Openai {
        private String apiKey = "";
        @Valid
        private Whisper whisper = new Whisper();
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public Whisper getWhisper() { return whisper; }
        public void setWhisper(Whisper v) { this.whisper = v; }

        public static class Whisper {
            private boolean enabled = false;
            @NotBlank
            private String url = "https://api.openai.com/v1/audio/transcriptions";
            private String model = "whisper-1";
            private int timeoutMs = 30000;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
            public String getUrl() { return url; }
            public void setUrl(String v) { this.url = v; }
            public String getModel() { return model; }
            public void setModel(String v) { this.model = v; }
            public int getTimeoutMs() { return timeoutMs; }
            public void setTimeoutMs(int v) { this.timeoutMs = v; }
        }
    }

    public static class Elevenlabs {
        private boolean enabled = false;
        private String apiKey = "";
        private String voiceId = "";
        private String model = "eleven_multilingual_v2";
        private String baseUrl = "https://api.elevenlabs.io/v1";
        private int timeoutMs = 30000;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getVoiceId() { return voiceId; }
        public void setVoiceId(String v) { this.voiceId = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int v) { this.timeoutMs = v; }
    }
}


