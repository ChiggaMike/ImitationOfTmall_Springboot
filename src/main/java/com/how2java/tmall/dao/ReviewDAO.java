package com.how2java.tmall.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.Review;

public interface ReviewDAO extends JpaRepository<Review, Integer>{

	public List<Review> findByProductOrderByIdDesc(Product product);
	public int countByProduct(Product product);
}
