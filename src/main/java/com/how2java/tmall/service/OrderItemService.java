package com.how2java.tmall.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.how2java.tmall.dao.OrderItemDAO;
import com.how2java.tmall.pojo.Order;
import com.how2java.tmall.pojo.OrderItem;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.User;

@Service
@CacheConfig(cacheNames="orderItems")
public class OrderItemService {

	@Autowired
	OrderItemDAO orderItemDAO;
	@Autowired
	ProductImageService productImageService;
	
	@CacheEvict(allEntries=true)
	public void add(OrderItem bean){
		orderItemDAO.save(bean);
	}
	
	@CacheEvict(allEntries=true)
	public void delete(int id){
		orderItemDAO.delete(id);
	}
	
	@Cacheable(key="'orderItems-one-'+ #p0")
	public OrderItem get(int id){
		return orderItemDAO.findOne(id);
	}
	
	@CacheEvict(allEntries=true)
	public void update(OrderItem bean){
		orderItemDAO.save(bean);
	}
	
	//根据订单项，填充订单的方法
	public void fill(List<Order> orders){
		for (Order order:orders){
			fill(order);
		}
	}
	//根据订单项，填充订单的方法
	public void fill(Order order){
		List<OrderItem> orderItems = listByOrder(order);
		float total = 0;
		int totalNumber = 0;
		for (OrderItem oi:orderItems){
			total += oi.getNumber() * oi.getProduct().getPromotePrice();
			totalNumber += oi.getNumber();
			productImageService.setFirstProductImage(oi.getProduct());
		}
		order.setTotal(total);
		order.setTotalNumber(totalNumber);
		order.setOrderItems(orderItems);
	}
	
	@Cacheable(key="'orderItems-oid-'+ #p0.id")
	public List<OrderItem> listByOrder(Order order){
		return orderItemDAO.findByOrderOrderByIdDesc(order);
	}
	
	@Cacheable(key="'orderItems-pid-'+ #p0.id")
	public List<OrderItem> listByProduct(Product product){
		return orderItemDAO.findByProduct(product);
	}
	
	public int getSaleCount(Product product){
		List<OrderItem> ois = listByProduct(product);
		int result = 0;
		for (OrderItem oi:ois){
			if (oi.getOrder() != null && oi.getOrder().getPayDate() != null){
				result += oi.getNumber();
			}
		}
		return result;
	}
	
	@Cacheable(key="'orderItems-uid-'+ #p0.id")
	public List<OrderItem> listByUser(User user){
		return orderItemDAO.findByUserAndOrderIsNull(user);
	}
}
