package com.integrityfamily.capital.dto;

/**
 * Resultado de un indicador SMFF.
 *
 * @param id          identificador canónico (IND-01 … IND-20)
 * @param name        nombre corto del indicador
 * @param group       grupo temático: cohesion | confianza | transf | resil | long
 * @param cls         clase: RESULTADO | PROCESO | ESTADO
 * @param value       puntuación normalizada 0–100
 * @param rawValue    valor sin normalizar (porcentaje, días, delta, etc.)
 * @param rawUnit     unidad del rawValue ("%" | "días" | "pts" | "entradas" …)
 * @param isEstimated true cuando el valor proviene de un fallback por falta de datos reales
 * @param dataPoints  cantidad de registros fuente usados en el cálculo
 */
public record IndicatorResult(
        String  id,
        String  name,
        String  group,
        String  cls,
        double  value,
        double  rawValue,
        String  rawUnit,
        boolean isEstimated,
        long    dataPoints
) {
    /** Fábrica rápida para indicadores con dato real */
    public static IndicatorResult real(String id, String name, String group, String cls,
                                       double value, double raw, String unit, long dataPoints) {
        return new IndicatorResult(id, name, group, cls, value, raw, unit, false, dataPoints);
    }

    /** Fábrica para fallback por falta de datos */
    public static IndicatorResult estimated(String id, String name, String group, String cls,
                                            double value) {
        return new IndicatorResult(id, name, group, cls, value, value, "%", true, 0);
    }
}
