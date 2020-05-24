package com.ranjay.bootstrap.repository;

import java.util.List;

import com.ranjay.bootstrap.model.Task;
import com.ranjay.bootstrap.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>{

	List<Task> findByUser(User user);
    
}