package com.ranjay.bootstrap.repository;

import com.ranjay.bootstrap.model.Product;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends CrudRepository<Product,Long>{
    
    Product findByName(String name);
}