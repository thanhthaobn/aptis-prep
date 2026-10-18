package com.aptisprep.entity;

import jakarta.persistence.*;

@Entity @Table(name = "certificates")
public class CertificateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, columnDefinition = "nvarchar(255)") private String name;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false, length = 30) private String status;
    protected CertificateEntity() {}
    public Integer getId() { return id; } public String getName() { return name; }
}
