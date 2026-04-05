package com.integrityfamily.assessment.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Question: Representa los reactivos del Assessment familiar.
 * Sincronizada con el QuestionLoaderService del Nodo Armenia.
 */
@Entity
@Table(name = "questions")
@Getter @Setter 
@NoArgsConstructor @AllArgsConstructor 
@Builder
public class Question {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String questionKey;
    
    @Column(columnDefinition = "TEXT")
    private String text;
    
    private String category;
    private String dimension;
    private String area;
    private String type; 

    @Builder.Default
    private Boolean active = true; // <--- ESTO MATA EL ÚLTIMO ERROR
}