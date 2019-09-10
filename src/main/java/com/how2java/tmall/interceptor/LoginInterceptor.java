package com.how2java.tmall.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.how2java.tmall.pojo.User;

public class LoginInterceptor implements HandlerInterceptor{

	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception{
		HttpSession session = request.getSession();
		String contextPath = session.getServletContext().getContextPath();
		String[] requireAuthPages = new String[]{
				"buy",
				"alipay",
				"payed",
				"cart",
				"bought",
				"confirmPay",
				"orderConfirmed",
				"forebuyone",
				"forebuy",
				"foreaddCart",
				"forecart",
				"forechangeOrderItem",
				"foredeleteOrderItem",
				"forecreateOrder",
				"forepayed",
				"forebought",
				"foreconfirmPay",
				"foreorderConfirmed",
				"foredeleteOrder",
				"forereview",
				"foredoreview"
		};
		String uri = request.getRequestURI();
		uri = StringUtils.remove(uri, contextPath + "/");
		String page = uri;
		if (beginWith(page, requireAuthPages)){
			Subject subject = SecurityUtils.getSubject();
			if (!subject.isAuthenticated()){
				response.sendRedirect("login");
				return false;
			}
		}
		return true;
	}
	
	private boolean beginWith(String page, String[] requireAuthPages){
		boolean result = false;
		for (String requireAuthPage:requireAuthPages){
			if (StringUtils.startsWith(page, requireAuthPage)){
				result = true;
				break;
			}
		}
		return result;
	}
	
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, 
			ModelAndView modelAndView) throws Exception{
		
	}
	
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
			Object o, Exception e) throws Exception{
		
	}
	
		
}
