package com.integrityfamily.family.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "family_members")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "role_type", length = 50)
    private String roleType;

    @Column(name = "age")
    private Integer age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Family family;

    public Member() {}

    // Manual Getters/Setters para compatibilidad total con el sistema actual
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
}