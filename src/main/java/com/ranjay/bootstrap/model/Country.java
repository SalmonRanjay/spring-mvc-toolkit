package com.ranjay.bootstrap.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class Country {
    
    @Id
    // @GeneratedValue(strategy = GenerationType.AUTO)
    @GeneratedValue
    private Integer id;
    private String name;
    private String capital;

    public Country(String name, String capital) {
        this.name = name;
        this.capital = capital;
    }


    
}