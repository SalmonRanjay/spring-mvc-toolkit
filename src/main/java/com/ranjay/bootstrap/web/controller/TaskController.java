package com.ranjay.bootstrap.web.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import com.ranjay.bootstrap.model.Task;
import com.ranjay.bootstrap.service.TaskService;
import com.ranjay.bootstrap.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class TaskController {

    @Autowired
    private TaskService taskService;
    private static final String TASK_FORM = "views/taskForm";

    @Autowired
    private UserService userService;

    @GetMapping(value = "/addTask")
    public String taskForm(@RequestParam String email, Model model, HttpSession session) {
        session.setAttribute("email", email);
        model.addAttribute("task", new Task());

        return TASK_FORM;
    }

    @PostMapping(value = "/addTask")
    public String addTask(@Valid Task task, BindingResult bindingResult, HttpSession session) {

        if (bindingResult.hasErrors()) {
            return TASK_FORM;
        }
        String email = (String) session.getAttribute("email");
        taskService.addTask(task, userService.findOne(email));
        return "redirect:/users";
    }
}