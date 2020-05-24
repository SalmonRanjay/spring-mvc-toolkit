package com.ranjay.bootstrap.web.controller;

import java.security.Principal;



import com.ranjay.bootstrap.model.User;
import com.ranjay.bootstrap.service.TaskService;
import com.ranjay.bootstrap.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class ProfileController {
    
    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @GetMapping(value="/profile")
    public String showProfile(Model model, Principal principal) {
        
        // user principal service to get 
        String email = principal.getName();

        // fetch user using email / ID
        User user = userService.findOne(email);
        model.addAttribute("tasks", taskService.findUserTask(user));
        return "views/profile";
    }
    
}