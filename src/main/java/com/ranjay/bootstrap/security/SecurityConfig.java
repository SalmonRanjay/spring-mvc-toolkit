package com.ranjay.bootstrap.security;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private DataSource dataSource;

    private static final String LOGIN_ROUTE = "/login";

    private final String[] PERMITTED_PAGES = new String[] { "/register","/","/about", LOGIN_ROUTE, "/css/**",
            "/webjars/**" };

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication().dataSource(dataSource)
        .usersByUsernameQuery("Select email as principal, password as credentials, true from user where email=?")
        .authoritiesByUsernameQuery("select user_email as principal, role_name as role from user_roles where user_email=?")
        .passwordEncoder(passwordEncoder()).rolePrefix("ROLE_");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
       
        // allow accuess to permited pages
        http.authorizeRequests().antMatchers(PERMITTED_PAGES).permitAll()
        .anyRequest().authenticated()
        .and().formLogin().loginPage(LOGIN_ROUTE).permitAll()
        .defaultSuccessUrl("/profile")
        .and().logout().logoutSuccessUrl(LOGIN_ROUTE);
    }
}