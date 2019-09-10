package com.how2java.tmall.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.how2java.tmall.comparator.ProductAllComparator;
import com.how2java.tmall.comparator.ProductDateComparator;
import com.how2java.tmall.comparator.ProductPriceComparator;
import com.how2java.tmall.comparator.ProductReviewComparator;
import com.how2java.tmall.comparator.ProductSaleComparator;
import com.how2java.tmall.pojo.Category;
import com.how2java.tmall.pojo.Order;
import com.how2java.tmall.pojo.OrderItem;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.ProductImage;
import com.how2java.tmall.pojo.PropertyValue;
import com.how2java.tmall.pojo.Review;
import com.how2java.tmall.pojo.User;
import com.how2java.tmall.service.CategoryService;
import com.how2java.tmall.service.OrderItemService;
import com.how2java.tmall.service.OrderService;
import com.how2java.tmall.service.ProductImageService;
import com.how2java.tmall.service.ProductService;
import com.how2java.tmall.service.PropertyValueService;
import com.how2java.tmall.service.ReviewService;
import com.how2java.tmall.service.UserService;
import com.how2java.tmall.util.Result;

@RestController
public class ForeRestController {

	@Autowired
	CategoryService categoryService;
	@Autowired
	ProductService productService;
	@Autowired
	UserService userService;
	@Autowired
	ProductImageService productImageService;
	@Autowired
	PropertyValueService propertyValueService;
	@Autowired
	ReviewService reviewService;
	@Autowired
	OrderService orderService;
	@Autowired
	OrderItemService orderItemService;
	
	//主页
	@GetMapping("/forehome")
	public Object home(){
		List<Category> cs = categoryService.list();
		productService.fill(cs);
		productService.fillByRow(cs);
		categoryService.removeCategoryFromProduct(cs);
		return cs;
	}
	
	//注册时用的方法
	//用了shiro的加密
	//随机方式创建盐，然后md5加密算法进行2次加密
	@PostMapping("/foreregister")
	public Object register(@RequestBody User user){
		String name = user.getName();
		String password = user.getPassword();
		name = HtmlUtils.htmlEscape(name);
		
		boolean exist = userService.isExist(name);
		if (exist){
			String message = "用户名已经被使用，请更换一个用户名";
			return Result.fail(message);
		}
		user.setName(name);
		String salt = new SecureRandomNumberGenerator().nextBytes().toString();
		String encodedPassword = new SimpleHash("md5", password, salt, 2).toString();
		user.setSalt(salt);
		user.setPassword(encodedPassword);
		userService.add(user);
		return Result.success();
	}
	
	//登录用的方法
	@PostMapping("/forelogin")
	public Object login(@RequestBody User userParam, HttpSession session){
		String name = userParam.getName();
		name = HtmlUtils.htmlEscape(name);
		//这里的subject其实就是user
		Subject subject = SecurityUtils.getSubject();
		UsernamePasswordToken token = new UsernamePasswordToken(name, userParam.getPassword());
		try{
			subject.login(token);
			User user = userService.getByName(name);
			session.setAttribute("user", user);
			return Result.success();
		}catch(AuthenticationException e){
			String message = "账号密码错误";
			return Result.fail(message);
		}
	}
	
	//修改密码时用的方法
	@PostMapping("/forerevisePassword")
	public Object revisePassword(@RequestBody User userParam, String newPassword){
		String name = userParam.getName();
		name = HtmlUtils.htmlEscape(name);
		Subject subject = SecurityUtils.getSubject();
		UsernamePasswordToken token = new UsernamePasswordToken(name, userParam.getPassword());
		try{
			subject.login(token);
			User user = userService.getByName(name);
			String salt = new SecureRandomNumberGenerator().nextBytes().toString();
			String newEncodedPassword = new SimpleHash("md5", newPassword, salt, 2).toString();
			user.setSalt(salt);
			user.setPassword(newEncodedPassword);
			userService.update(user);
			return Result.success();
		}catch(AuthenticationException e){
			String message = "旧账号密码错误";
			return Result.fail(message);
		}
//		不用shiro时写的改账号密码方法：
//		String name = userParam.getName();
//		name = HtmlUtils.htmlEscape(name);
//		User user = userService.get(name, userParam.getPassword());
//		if (user == null){
//			String message = "旧账号密码错误";
//			return Result.fail(message);
//		}
//		else{
//			newPassword = HtmlUtils.htmlEscape(newPassword);
//			user.setPassword(newPassword);
//			userService.update(user);
//			return Result.success();
//		}
	}
	
	//进入产品页时用的方法
	@GetMapping("/foreproduct/{pid}")
	public Object product(@PathVariable("pid") int pid){
		Product product = productService.get(pid);
		List<ProductImage> productSingleImages = productImageService.listSingleProductImages(product);
		List<ProductImage> productDetailImages = productImageService.listDetailProductImages(product);
		product.setProductSingleImages(productSingleImages);
		product.setProductDetailImages(productDetailImages);
		List<PropertyValue> pvs = propertyValueService.list(product);
		List<Review> reviews = reviewService.list(product);
		productService.setSaleAndReviewNumber(product);
		productImageService.setFirstProductImage(product);
		Map<String, Object> map = new HashMap<>();
		map.put("product", product);
		map.put("pvs", pvs);
		map.put("reviews", reviews);
		return Result.success(map);
	}
	
	//查看是否已登录的方法，如果未登录则弹出模态登录div
	@GetMapping("forecheckLogin")
	public Object checkLogin(HttpSession session){
		Subject subject = SecurityUtils.getSubject();
		if (!subject.isAuthenticated()){
			return Result.success();
		}
		else{
			return Result.fail("未登录");
		}
	}
	
	//进入分类页时用的方法
	@GetMapping("forecategory/{cid}")
	public Object category(@PathVariable int cid, String sort){
		Category c = categoryService.get(cid);
		productService.fill(c);
		productService.setSaleAndReviewNumber(c.getProducts());
		categoryService.removeCategoryFromProduct(c);
		
		if (sort != null){
			switch(sort){
			case "review":
				Collections.sort(c.getProducts(), new ProductReviewComparator());
				break;
			case "price":
				Collections.sort(c.getProducts(), new ProductPriceComparator());
				break;
			case "date":
				Collections.sort(c.getProducts(), new ProductDateComparator());
				break;
			case "saleCount":
				Collections.sort(c.getProducts(), new ProductSaleComparator());
				break;
			case "all":
				Collections.sort(c.getProducts(), new ProductAllComparator());
				break;
			}
		}
		return c;
	}
	
	//搜索时用的方法
	@PostMapping("foresearch")
	public Object search(String keyword){
		if (keyword == null){
			keyword = "";
		}
		List<Product> ps = productService.search(keyword, 0, 20);
		productImageService.setFirstProductImage(ps);
		productService.setSaleAndReviewNumber(ps);
		return ps;
	}
	
	//点击立即购买时用的方法,然后前端会跳转到结算页面
	@GetMapping("forebuyone")
	public Object buyone(int pid, int num, HttpSession session){
		return buyoneAndAddCart(pid, num, session);
	}
	//点击立即购买或者加入购物车时用的方法，主要为了生成新的orderItem或在原有的orderItems上增加要买的此商品的数量
	private int buyoneAndAddCart(int pid, int num, HttpSession session){
		Product product = productService.get(pid);
		int oiid = 0;
		User user = (User) session.getAttribute("user");
		boolean found = false;
		List<OrderItem> ois = orderItemService.listByUser(user);
		//如果这商品已经存在在购物车里了，那么点击立即购买的话
		//要把立即购买此商品的数量加上本来在购物车里的此商品的数量
		for (OrderItem oi:ois){
			if (oi.getProduct().getId() == product.getId()){
				oi.setNumber(oi.getNumber() + num);
				orderItemService.update(oi);
				found = true;
				oiid = oi.getId();
				break;
			}
		}
		//如果这商品并没有在购物车里（或者是不存在此orderItem）, 则新建orderItem
		if (!found){
			OrderItem oi = new OrderItem();
			oi.setUser(user);
			oi.setProduct(product);
			oi.setNumber(num);
			orderItemService.add(oi);
			oiid = oi.getId();
		}
		return oiid;
	}
	
	//点击加入购物车时用的方法
	@GetMapping("foreaddCart")
	public Object addCart(int pid, int num, HttpSession session){
		buyoneAndAddCart(pid, num, session);
		return Result.success();
	}
	
	//点击查看购物车时用的方法
	@GetMapping("forecart")
	public Object cart(HttpSession session){
		User user = (User) session.getAttribute("user");
		List<OrderItem> ois = orderItemService.listByUser(user);
		productImageService.setFirstProductImagesOnOrderItems(ois);
		return ois;
	}
	
	//显示结算页面时用到的方法
	//因为结算页面不仅在立即购买页面之后要用，还在购物车页面之后要用，所以有可能有多条orderItem，所以用数组
	@GetMapping("forebuy")
	public Object buy(String[] oiid, HttpSession session){
		//用来储存所有orderItem的list
		List<OrderItem> orderItems = new ArrayList<>();
		float total = 0;
		
		for (String strid:oiid){
			int id = Integer.parseInt(strid);
			OrderItem oi = orderItemService.get(id);
			total += oi.getProduct().getPromotePrice() * oi.getNumber();
			orderItems.add(oi);
		}
		productImageService.setFirstProductImagesOnOrderItems(orderItems);
		session.setAttribute("ois", orderItems);
		
		Map<String, Object> map = new HashMap<>();
		map.put("orderItems", orderItems);
		map.put("total", total);
		return Result.success(map);
	}
	
	//购物车页面修改商品数量时用的方法
	@GetMapping("forechangeOrderItem")
	public Object changeOrderItem(HttpSession session, int pid, int num){
		User user = (User) session.getAttribute("user");
		if (user == null){
			return Result.fail("未登录");
		}
		List<OrderItem> ois = orderItemService.listByUser(user);
		for (OrderItem oi:ois){
			if (oi.getProduct().getId() == pid){
				oi.setNumber(num);
				orderItemService.update(oi);
				break;
			}
		}
		return Result.success();
	}
	
	//购物车页面删除订单项时用的方法
	@GetMapping("foredeleteOrderItem")
	public Object deleteOrderItem(HttpSession session, int oiid){
		User user = (User) session.getAttribute("user");
		if (user == null){
			return Result.fail("未登录");
		}
		orderItemService.delete(oiid);
		return Result.success();
	}
	
	//点击结算页面的提交订单按钮时用的方法
	@PostMapping("forecreateOrder")
	public Object createOrder(@RequestBody Order order, HttpSession session){
		User user = (User) session.getAttribute("user");
		if (user == null){
			return Result.fail("未登录");
		}
		String orderCode = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + RandomUtils.nextInt(100);
		order.setOrderCode(orderCode);
		order.setCreateDate(new Date());
		order.setUser(user);
		order.setStatus(OrderService.waitPay);
		List<OrderItem> ois = (List<OrderItem>) session.getAttribute("ois");
		float total = orderService.add(order, ois);
		Map<String, Object> map = new HashMap<>();
		map.put("oid", order.getId());
		map.put("total", total);
		return Result.success(map);
	}
	
	//点击付款页面的确认支付按钮时用的方法
	@GetMapping("forepayed")
	public Object payed(int oid){
		Order order = orderService.get(oid);
		order.setStatus(OrderService.waitDelivery);
		order.setPayDate(new Date());
		orderService.update(order);
		return order;
	}
	
	//我的订单页面用的方法
	@GetMapping("forebought")
	public Object bought(HttpSession session){
		User user = (User) session.getAttribute("user");
		if (user == null){
			return Result.fail("未登录");
		}
		List<Order> os = orderService.listByUserWithoutDelete(user);
		orderService.removeOrderFromOrderItem(os);
		return os;
	}
	
	//确认收货页面用的方法
	@GetMapping("foreconfirmPay")
	public Object confirmPay(int oid){
		Order o = orderService.get(oid);
		orderItemService.fill(o);
		orderService.cal(o);
		orderService.removeOrderFromOrderItem(o);
		return o;
	}
	
	//点击确认收货按钮时用的方法
	@GetMapping("foreorderConfirmed")
	public Object orderConfirmed(int oid){
		Order o = orderService.get(oid);
		o.setStatus(OrderService.waitReview);
		o.setConfirmDate(new Date());
		orderService.update(o);
		return Result.success();
	}
	
	//“我的订单”页面删除订单时用的方法（实际上订单数据并没有删除，只是把状态改为delete）
	@GetMapping("foredeleteOrder")
	public Object deleteOrder(int oid){
		Order o = orderService.get(oid);
		o.setStatus(OrderService.delete);
		orderService.update(o);
		return Result.success();
	}
	
	//评价页面用的方法
	@GetMapping("forereview")
	public Object review(int oid){
		Order o = orderService.get(oid);
		orderItemService.fill(o);
		orderService.removeOrderFromOrderItem(o);
		Product p = o.getOrderItems().get(0).getProduct();
		List<Review> reviews = reviewService.list(p);
		productService.setSaleAndReviewNumber(p);
		Map<String, Object> map = new HashMap<>();
		map.put("p", p);
		map.put("reviews", reviews);
		map.put("o", o);
		return Result.success(map);
	}
	
	//点击提交评价按钮时用的方法
	@GetMapping("foredoreview")
	public Object doreivew(HttpSession session, int oid, int pid, String content){
		Order o = orderService.get(oid);
		o.setStatus(OrderService.finish);
		orderService.update(o);
		
		Product p = productService.get(pid);
		content = HtmlUtils.htmlEscape(content);
		
		User user = (User) session.getAttribute("user");
		Review review = new Review();
		review.setContent(content);
		review.setUser(user);
		review.setProduct(p);
		review.setCreateDate(new Date());
		reviewService.add(review);
		return Result.success();
	}
	
}
