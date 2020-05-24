package com.ranjay.bootstrap.web.controller;

import com.ranjay.bootstrap.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class UserController {
    
    @Autowired
    private UserService userService;

    @GetMapping(value="/users")
    public String listUsers(Model model, @RequestParam( defaultValue = "",name = "name") String name  ) {
        model.addAttribute("users", userService.findByName(name));
        return "views/list";
    }
    
}