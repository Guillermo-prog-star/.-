package com.integrityfamily.family.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Member: Entidad sincronizada con MemberService.
 * Define la estructura de datos para los integrantes de Integrity Family en Armenia.
 */
@Entity
@Table(name = "members")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName; 

    @Column(nullable = false)
    private String lastName;  

    @Column(nullable = false)
    private String relationship; 

    private Integer age;
    private String gender;
    private String occupation;
    private String role;      // Crucial para la lógica del Service
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    @JsonIgnore 
    private Family family;
}