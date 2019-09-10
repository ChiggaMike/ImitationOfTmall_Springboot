package com.how2java.tmall.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.how2java.tmall.pojo.User;

public interface UserDAO extends JpaRepository<User, Integer>{

	public User findByName(String name);
	public User getByNameAndPassword(String name, String password);
	
}
