package com.ranjay.bootstrap.repository;

import com.ranjay.bootstrap.model.ShoppingCart;

import org.springframework.data.repository.CrudRepository;

public interface ShoppingCartRepositor extends CrudRepository<ShoppingCart,Long> {
    
}