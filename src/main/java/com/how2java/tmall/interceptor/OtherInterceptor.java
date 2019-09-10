package com.how2java.tmall.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.how2java.tmall.pojo.Category;
import com.how2java.tmall.pojo.OrderItem;
import com.how2java.tmall.pojo.User;
import com.how2java.tmall.service.CategoryService;
import com.how2java.tmall.service.OrderItemService;

public class OtherInterceptor implements HandlerInterceptor{

	@Autowired
	CategoryService categoryService;
	@Autowired
	OrderItemService orderItemService;
	
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
			Object o) throws Exception{
		return true;
	}
	
	public void postHandle(HttpServletRequest request, HttpServletResponse response, 
			Object o, ModelAndView modelAndView) throws Exception{
		HttpSession session = request.getSession();
		User user = (User) session.getAttribute("user");
		int cartTotalItemNumber = 0;
		if (user != null){
			List<OrderItem> ois = orderItemService.listByUser(user);
			for (OrderItem oi:ois){
				cartTotalItemNumber += oi.getNumber();
			}
		}
		List<Category> cs = categoryService.list();
		String contextPath = request.getServletContext().getContextPath();
		request.getServletContext().setAttribute("categories_below_search", cs);
		session.setAttribute("cartTotalItemNumber", cartTotalItemNumber);
		request.getServletContext().setAttribute("contextPath", contextPath);
	}
	
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
			Object o, Exception e) throws Exception{
		
	}
}
