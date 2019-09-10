package com.how2java.tmall.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.how2java.tmall.pojo.Order;
import com.how2java.tmall.pojo.OrderItem;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.User;

public interface OrderItemDAO extends JpaRepository<OrderItem, Integer>{

	public List<OrderItem> findByOrderOrderByIdDesc(Order order);
	public List<OrderItem> findByProduct(Product product);
	public List<OrderItem> findByUserAndOrderIsNull(User user);
}
