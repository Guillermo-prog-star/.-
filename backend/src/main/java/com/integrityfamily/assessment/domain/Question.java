package com.integrityfamily.assessment.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text", nullable = false, length = 500)
    private String text;

    @Column(name = "dimension", nullable = false, length = 50)
    private String dimension;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Question() {}

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getDimension() { return dimension; }
    public void setDimension(String dim) { this.dimension = dim; }
    public String getCategory() { return category; }
    public void setCategory(String cat) { this.category = cat; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer order) { this.sortOrder = order; }
}