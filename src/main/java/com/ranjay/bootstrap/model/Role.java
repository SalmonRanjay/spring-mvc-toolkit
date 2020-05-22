package com.ranjay.bootstrap.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class Role {

    @Id
    private String name;

    @ManyToMany(mappedBy = "roles")
    private List<User> users;

    public Role(String name, List<User> users) {
        this.name = name;
        this.users = users;
    }

    public Role(String name) {
        this.name = name;
    }

    
}
