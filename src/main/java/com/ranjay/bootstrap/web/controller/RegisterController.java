package com.ranjay.bootstrap.web.controller;

import javax.validation.Valid;

import com.ranjay.bootstrap.model.User;
import com.ranjay.bootstrap.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
public class RegisterController {

    @Autowired
    private UserService userService;
    private static final String REGISTER_FORM = "views/registerForm";
    

    @GetMapping("/register")
    public String registerForm(Model model){

        model.addAttribute("user", new User());
        return REGISTER_FORM;
    }

    @PostMapping(value="/register")
    public String registerUser(@Valid User user,BindingResult bindingResult, Model model) {
        
        if(bindingResult.hasErrors()){

            return REGISTER_FORM;
        }
        if(userService.isUserPresent(user.getEmail())){
            model.addAttribute("exists", true);
            return REGISTER_FORM;
        }
        userService.createUser(user);

        return "views/success";
    }
    
}