package com.ranjay.bootstrap.repository;

import com.ranjay.bootstrap.model.CartItem;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends CrudRepository<CartItem,Long> {
    
}