package com.integrityfamily.family.dto;
import jakarta.validation.constraints.*;
public record FamilyCreateRequest(
    @NotBlank @Size(max=120) String name,
    @Size(max=255) String description,
    String municipio, String whatsapp, String pin) {}


