package com.ranjay.bootstrap.repository;

import com.ranjay.bootstrap.model.Country;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CountryRepository extends JpaRepository<Country,Integer> {
    
}