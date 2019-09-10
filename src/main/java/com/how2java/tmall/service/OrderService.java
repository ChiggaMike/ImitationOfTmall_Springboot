package com.how2java.tmall.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.how2java.tmall.dao.OrderDAO;
import com.how2java.tmall.pojo.Order;
import com.how2java.tmall.pojo.OrderItem;
import com.how2java.tmall.pojo.User;
import com.how2java.tmall.util.Page4Navigator;

@Service
@CacheConfig(cacheNames="orders")
public class OrderService {

	public static final String waitPay = "waitPay";
	public static final String waitDelivery = "waitDelivery";
	public static final String waitConfirm = "waitConfirm";
	public static final String waitReview = "waitReview";
	public static final String finish = "finish";
	public static final String delete = "delete";
	
	@Autowired
	OrderDAO orderDAO;
	@Autowired
	OrderItemService orderItemService;
	
	@Cacheable(key="'orders-page-'+ #p0+ '-'+ #p1")
	public Page4Navigator<Order> list(int start, int size, int navigatePages){
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		Pageable pageable = new PageRequest(start, size, sort);
		Page pageFromJPA = orderDAO.findAll(pageable);
		return new Page4Navigator(pageFromJPA, navigatePages);
	}
	
	public void removeOrderFromOrderItem(List<Order> orders){
		for (Order order:orders){
			removeOrderFromOrderItem(order);
		}
	}
	
	public void removeOrderFromOrderItem(Order order){
		List<OrderItem> orderItems = order.getOrderItems();
		for (OrderItem orderItem:orderItems){
			orderItem.setOrder(null);
		}
	}
	
	@Cacheable(key="'orders-one-'+ #p0")
	public Order get(int id){
		return orderDAO.findOne(id);
	}
	
	@CacheEvict(allEntries=true)
	public void update(Order bean){
		orderDAO.save(bean);
	}
	
	//创建订单，用事务管理
	@CacheEvict(allEntries=true)
	@Transactional(propagation = Propagation.REQUIRED, rollbackForClassName = "Exception")
	public float add(Order order, List<OrderItem> ois){
		float total = 0;
		orderDAO.save(order);
		if (false)
			throw new RuntimeException();
		for (OrderItem oi:ois){
			oi.setOrder(order);
			orderItemService.update(oi);
			total += oi.getProduct().getPromotePrice() * oi.getNumber();
		}
		return total;
	}
	
	//用于“我的订单”页面，显示订单状态为非“已删除”的订单
	@Cacheable("'orders-uid-'+ #p0.id")
	public List<Order> listByUserWithoutDelete(User user){
		List<Order> os = orderDAO.findByUserAndStatusNotOrderByIdDesc(user, OrderService.delete);
		orderItemService.fill(os);
		return os;
	}
	
	//用于计算订单总金额
	public void cal(Order o){
		List<OrderItem> orderItems = o.getOrderItems();
		float total = 0;
		for (OrderItem oi:orderItems){
			total += oi.getProduct().getPromotePrice() * oi.getNumber();
		}
		o.setTotal(total);
	}
}
