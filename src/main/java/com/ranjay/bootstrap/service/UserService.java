package com.ranjay.bootstrap.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ranjay.bootstrap.model.Role;
import com.ranjay.bootstrap.model.User;
import com.ranjay.bootstrap.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;

    public void createUser(User user){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        Role userRole = new Role("USER");
        List<Role> roles = new ArrayList<>();
        roles.add(userRole);
        user.setRoles(roles);
        // need to save role here
        userRepository.save(user);

    }
    public void createAdmin(User user){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        user.setPassword(encoder.encode(user.getPassword()));
        Role userRole = new Role("ADMIN");
        List<Role> roles = new ArrayList<>();
        roles.add(userRole);
        user.setRoles(roles);
        // need to save role here
        userRepository.save(user);

    }

    public User findOne(String email){
        Optional<User> user =  userRepository.findById(email);
        if(user.isPresent())
            return user.get();
        return null;
    }
	public boolean isUserPresent(String email) {
        Optional<User> user =  userRepository.findById(email);
        if(user.isPresent()){
            return true;
        }else{
            return false;
        }
	}
	public List<User> findAll() {
		return userRepository.findAll();
	}
	public List<User> findByName(String name) {
		return userRepository.findByNameLike("%" + name + "%");
	}


}