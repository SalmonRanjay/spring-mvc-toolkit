package com.ranjay.bootstrap.web.controller;

import com.ranjay.bootstrap.model.Country;
import com.ranjay.bootstrap.repository.CountryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class CountryController {
    
    @Autowired
    private CountryRepository countryRepository;

    @GetMapping(value="/")
    public String showPage(Model model, @RequestParam(defaultValue = "0",name = "page") int page) {

        model.addAttribute("currentPage", page);
        model.addAttribute("data", countryRepository.findAll(PageRequest.of(page, 10)));
        return "index";
    }

    @PostMapping("/save")
    public String save(Country country){
        countryRepository.save(country);

        return "redirect:/";
    }

    @GetMapping("/delete")
    public String deleteCountry(Integer id){
        Country country = countryRepository.findById(id).get();
        countryRepository.delete(country);

        return "redirect:/";
    }

    @GetMapping("/findOne")
    @ResponseBody
    public Country findOne(Integer id){
        Country country = countryRepository.findById(id).get();
        return country;
    }




    
}