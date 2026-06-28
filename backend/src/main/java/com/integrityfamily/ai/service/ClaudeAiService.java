package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.Family;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SDD-AI-BRIDGE: Claude Specialized Family Service.
 * Actúa como el córtex específico para la interacción por voz de la familia.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClaudeAiService {

    private final AiProvider aiProvider; // Inyecta ClaudeAiProvider por ser @Primary
    private final ContextSynthesizer contextSynthesizer;

    /**
     * Procesa una transcripción de voz y genera una respuesta empática 
     * basada en el contexto actual de la familia.
     */
    public String generateFamilyResponse(String transcription, Family family) {
        log.info("🧠 [CLAUDE-SERVICE] Generando respuesta sistémica para familia: {}", family.getName());

        // 1. Sintetizar el contexto familiar (ICF, miembros, historia reciente)
        AiContext context = contextSynthesizer.synthesize(family, "NEUTRAL");

        // 2. Construir la instrucción específica para interacción por voz
        String voiceInstruction = String.format(
            "INTERACCIÓN POR VOZ: La familia dice: \"%s\". " +
            "Responde de forma breve, empática y directa (máximo 3 frases) " +
            "teniendo en cuenta su estado de integridad actual.",
            transcription
        );

        // 3. Invocar al modelo
        return aiProvider.generateResponse(voiceInstruction, context);
    }
}


