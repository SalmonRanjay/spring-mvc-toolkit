package com.ranjay.bootstrap.service;

import java.util.List;

import com.ranjay.bootstrap.model.Task;
import com.ranjay.bootstrap.model.User;
import com.ranjay.bootstrap.repository.TaskRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;

    public void addTask(Task task, User user){
        task.setUser(user);
        taskRepository.save(task);
    }

    public List<Task> findUserTask(User user){

        return taskRepository.findByUser(user);
    }
    
}