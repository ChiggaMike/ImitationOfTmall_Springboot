package com.how2java.tmall.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.how2java.tmall.dao.ProductImageDAO;
import com.how2java.tmall.pojo.OrderItem;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.ProductImage;

@Service
@CacheConfig(cacheNames="productImages")
public class ProductImageService {

	public static final String type_single = "single";
	public static final String type_detail = "detail";
	
	@Autowired
	ProductImageDAO productImageDAO;
	@Autowired
	ProductService productService;
	
	@CacheEvict(allEntries=true)
	public void add(ProductImage bean){
		productImageDAO.save(bean);
	}
	
	@CacheEvict(allEntries=true)
	public void delete(int id){
		productImageDAO.delete(id);
	}
	
	@Cacheable(key="'productImages-one-'+ #p0")
	public ProductImage get(int id){
		return productImageDAO.findOne(id);
	}
	
	@Cacheable(key="'productImages-single-pid-'+ #p0.id")
	public List<ProductImage> listSingleProductImages(Product product){
		return productImageDAO.findByProductAndTypeOrderByIdDesc(product, type_single);
	}
	
	@Cacheable(key="'productImages-detail-pid-'+ #p0.id")
	public List<ProductImage> listDetailProductImages(Product product){
		return productImageDAO.findByProductAndTypeOrderByIdDesc(product, type_detail);
	}
	
	public void setFirstProductImage(List<Product> products){
		for (Product product:products){
			setFirstProductImage(product);
		}
	}
	
	public void setFirstProductImage(Product product){
		List<ProductImage> singleImages = listSingleProductImages(product);
		if (!singleImages.isEmpty())
			product.setFirstProductImage(singleImages.get(0));
		else
			//这样做考虑到产品还没来得及设置图片，但是在订单后台管理里查看订单项的对应图片
			product.setFirstProductImage(new ProductImage());
	}
	
	//用于结算页面
	public void setFirstProductImagesOnOrderItems(List<OrderItem> ois){
		for (OrderItem oi:ois){
			setFirstProductImage(oi.getProduct());
		}
	}
	
	
}
