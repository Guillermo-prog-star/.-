package com.integrityfamily.ai.config;

import com.integrityfamily.ai.command.CommandHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class AiCommandConfig {

    @Bean
    public Map<String, CommandHandler> aiCommandRegistry(List<CommandHandler> handlers) {
        Map<String, CommandHandler> registry = new HashMap<>();
        for (CommandHandler handler : handlers) {
            registry.put(handler.getCommandName(), handler);
            
            // Registrar alias manuales si es necesario
            if (handler.getCommandName().equals("status")) {
                registry.put("diagnostico", handler);
            }
        }
        return registry;
    }
}


