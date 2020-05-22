package com.ranjay.bootstrap.repository;

import com.ranjay.bootstrap.model.Role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role,String> {
    
}