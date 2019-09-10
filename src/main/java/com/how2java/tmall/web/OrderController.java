package com.how2java.tmall.web;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.how2java.tmall.pojo.Order;
import com.how2java.tmall.service.OrderItemService;
import com.how2java.tmall.service.OrderService;
import com.how2java.tmall.util.Page4Navigator;
import com.how2java.tmall.util.Result;

@RestController
public class OrderController {

	@Autowired
	OrderService orderService;
	@Autowired
	OrderItemService orderItemService;
	
	@GetMapping("/orders")
	public Page4Navigator<Order> list(@RequestParam(value = "start", defaultValue = "0") int start, 
			@RequestParam(value = "size", defaultValue = "5") int size) throws Exception{
		start = start < 0 ? 0 : start;
		Page4Navigator<Order> page = orderService.list(start, size, 5);
		orderItemService.fill(page.getContent());
		//因为order转换为json时会把其对应的orderItems转换为json,
		//而orderItem里又有order属性
		//所以需要此方法来避免依赖循环
		orderService.removeOrderFromOrderItem(page.getContent());
		return page;
	}
	
	@PutMapping("deliveryOrder/{oid}")
	public Object deliveryOrder(@PathVariable int oid) throws IOException{
		Order o = orderService.get(oid);
		o.setDeliveryDate(new Date());
		o.setStatus(OrderService.waitConfirm);
		orderService.update(o);
		return Result.success();
	}
}
