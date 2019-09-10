package com.how2java.tmall.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.how2java.tmall.dao.ReviewDAO;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.Review;

@Service
@CacheConfig(cacheNames="reviews")
public class ReviewService {

	@Autowired
	ReviewDAO reviewDAO;
	@Autowired
	ProductService productService;
	
	@CacheEvict(allEntries=true)
	public void add(Review bean){
		reviewDAO.save(bean);
	}
	
	@Cacheable(key="'reviews-pid-'+ #p0.id")
	public List<Review> list(Product product){
		return reviewDAO.findByProductOrderByIdDesc(product);
	}
	
	@Cacheable(key="'review-count-pid-'+ #p0.id")
	public int getCount(Product product){
		return reviewDAO.countByProduct(product);
	}
}
