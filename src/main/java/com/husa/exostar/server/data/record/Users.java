package com.husa.exostar.server.data.record;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Users {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name="ID", nullable=false, unique=true)
    private Integer id;

    @Column(name="NAME", length=100, nullable=false)
    private String name;

    @Column(name="TELEPHONE", length=100, nullable=false)
    private String telephone;

    @Column(name="EMAIL", length=100, nullable=false)
    private String email;

}
