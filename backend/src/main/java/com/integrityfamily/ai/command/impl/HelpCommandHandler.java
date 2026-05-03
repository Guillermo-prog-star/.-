package com.integrityfamily.ai.command.impl;

import com.integrityfamily.ai.command.CommandHandler;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.Plan;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HelpCommandHandler implements CommandHandler {

    @Override
    public String handle(Family family, RiskSnapshot risk, List<Plan> plans, boolean sentinel) {
        return "### Ã°Å¸â€ºÂ Ã¯Â¸Â COMANDOS DISPONIBLES\n" +
               "- `/status`: Resumen del estado actual del nÃƒÂºcleo familiar.\n" +
               "- `/diagnostico`: Alias de status (anÃƒÂ¡lisis detallado).\n" +
               "- `/ayuda`: Muestra esta lista de comandos.";
    }

    @Override
    public String getCommandName() {
        return "ayuda";
    }
}


