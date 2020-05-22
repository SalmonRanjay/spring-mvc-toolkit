package com.ranjay.bootstrap.repository;

import com.ranjay.bootstrap.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    
}