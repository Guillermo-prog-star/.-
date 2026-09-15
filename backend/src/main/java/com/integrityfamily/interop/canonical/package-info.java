/**
 * Modelo Canónico de interoperabilidad (Fase 1 del programa de integración con
 * el ecosistema de salud colombiano).
 *
 * Estos tipos son deliberadamente independientes de cualquier estándar externo
 * (FHIR, HL7). El dominio de Integrity Family (Family, FamilyMember, Evaluation,
 * risk_trajectories, etc.) nunca depende de este paquete ni de FHIR directamente;
 * son los mappers de la capa de interoperabilidad (fases posteriores) los que
 * traducen el dominio hacia este modelo canónico, y de ahí hacia perfiles FHIR.
 * El objetivo es que un cambio de versión de FHIR (R4 → R5) o la aparición de
 * un nuevo consumidor (SISPRO, una IPS) nunca obligue a tocar el dominio.
 */
package com.integrityfamily.interop.canonical;
