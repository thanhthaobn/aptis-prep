package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity @Table(name = "scoring_schemes")
public class ScoringSchemeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "certificate_id", nullable = false) private Integer certificateId;
    @Column(nullable = false, columnDefinition = "nvarchar(255)") private String name;
    @Column(nullable = false, length = 30) private String version;
    @Column(nullable = false, length = 30) private String status;
    protected ScoringSchemeEntity() {}
    public Integer getId() { return id; } public String getName() { return name; }
    public String getVersion() { return version; }
}
