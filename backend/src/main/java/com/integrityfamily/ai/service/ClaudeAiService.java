package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.Family;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SDD-AI-BRIDGE: Claude Specialized Family Service.
 * ActÃƒÂºa como el cÃƒÂ³rtex especÃƒÂ­fico para la interacciÃƒÂ³n por voz de la familia.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaudeAiService {

    private final AiProvider aiProvider; // Inyecta ClaudeAiProvider por ser @Primary
    private final ContextSynthesizer contextSynthesizer;

    /**
     * Procesa una transcripciÃƒÂ³n de voz y genera una respuesta empÃƒÂ¡tica 
     * basada en el contexto actual de la familia.
     */
    public String generateFamilyResponse(String transcription, Family family) {
        log.info("Ã°Å¸Â§Â  [CLAUDE-SERVICE] Generando respuesta sistÃƒÂ©mica para familia: {}", family.getName());

        // 1. Sintetizar el contexto familiar (ICF, miembros, historia reciente)
        AiContext context = contextSynthesizer.synthesize(family, "NEUTRAL");

        // 2. Construir la instrucciÃƒÂ³n especÃƒÂ­fica para interacciÃƒÂ³n por voz
        String voiceInstruction = String.format(
            "INTERACCIÃƒâ€œN POR VOZ: La familia dice: \"%s\". " +
            "Responde de forma breve, empÃƒÂ¡tica y directa (mÃƒÂ¡ximo 3 frases) " +
            "teniendo en cuenta su estado de integridad actual.",
            transcription
        );

        // 3. Invocar al modelo
        return aiProvider.generateResponse(voiceInstruction, context);
    }
}


