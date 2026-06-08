package com.integrityfamily.dna.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FamilyDnaDto(
    Long familyId,
    List<String> valores,
    List<String> fortalezas,
    List<String> sombras,
    List<String> patrones,
    String estiloComunicacion,
    String ritmoFamiliar,
    List<PotencialMiembroDto> potencialOculto,
    String narrativaIa,
    int version,
    LocalDateTime updatedAt
) {
    public record PotencialMiembroDto(
        String miembro,
        String talento,
        String descripcion
    ) {}
}
