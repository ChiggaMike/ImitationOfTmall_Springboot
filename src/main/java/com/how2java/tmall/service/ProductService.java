package com.how2java.tmall.service;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.how2java.tmall.dao.ProductDAO;
import com.how2java.tmall.es.ProductESDAO;
import com.how2java.tmall.pojo.Category;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.util.Page4Navigator;
import com.how2java.tmall.util.SpringContextUtil;

//增加，删除，修改的时候，除了通过ProductDAO对数据库产生影响，还要通过ProductESDAO同步到es
@Service
@CacheConfig(cacheNames="products")
public class ProductService {

	@Autowired
	ProductDAO productDAO;
	@Autowired
	ProductESDAO productESDAO;
	@Autowired
	CategoryService categoryService;
	@Autowired
	ProductImageService productImageService;
	@Autowired
	OrderItemService orderItemService;
	@Autowired
	ReviewService reviewService;
	
	
	@Cacheable(key="'products-cid-'+ #p0+ '-page-'+ #p1 + '-'+ #p2")
	public Page4Navigator<Product> list(int cid, int start, int size, int navigatePages){
		Category c = categoryService.get(cid);
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		Pageable pageable = new PageRequest(start, size, sort);
		Page<Product> pageFromJPA = productDAO.findByCategory(c, pageable);
		return new Page4Navigator<>(pageFromJPA, navigatePages);
	}
	
	@CacheEvict(allEntries=true)
	public void add(Product bean){
		productDAO.save(bean);
		productESDAO.save(bean);
	}
	
	@CacheEvict(allEntries=true)
	public void delete(int id){
		productDAO.delete(id);
		productESDAO.delete(id);
	}
	
	@Cacheable(key="'product-one-'+ #p0")
	public Product get(int id){
		return productDAO.findOne(id);
	}

	@CacheEvict(allEntries=true)
	public void update(Product bean){
		productDAO.save(bean);
		productDAO.save(bean);
	}
	
	@Cacheable(key="'products-cid-'+ #p0.id")
	public List<Product> listByCategory(Category category){
		return productDAO.findByCategoryOrderById(category);
	}
	
	public void fill(List<Category> categories){
		for (Category category:categories){
			fill(category);
		}
	}
	//为Category填充products
	//这个 listByCategory 方法本来就是 ProductService 的方法，却不能直接调用,
	//因为springboot的缓存机制是通过切面编程aop来实现的,
	//从fill方法里直接调用 listByCategory方法，aop是拦截不到的，也就不会走缓存了,
	//所以要通过这种 绕一绕的方式故意诱发aop,这样才会想我们期望的那样走redis缓存。
	public void fill(Category category){
		ProductService productService = SpringContextUtil.getBean(ProductService.class);
		List<Product> products = productService.listByCategory(category);
		productImageService.setFirstProductImage(products);
		category.setProducts(products);
	}
	
	//为Category填充productsByRow
	public void fillByRow(List<Category> categories){
		int productNumberEachRow = 8;
		for (Category category:categories){
			List<Product> products = category.getProducts();
			List<List<Product>> productsByRow = new ArrayList<>();
			for (int i = 0; i < products.size(); i += productNumberEachRow){
				int size = i + productNumberEachRow;
				size = size > products.size() ? products.size() : size;
				List<Product> productsOfEachRow = products.subList(i, size);
				productsByRow.add(productsOfEachRow);
			}
			category.setProductsByRow(productsByRow);
		}
	}
	
	public void setSaleAndReviewNumber(List<Product> products){
		for (Product product:products){
			setSaleAndReviewNumber(product);
		}
	}
	
	//用于产品页，为产品设置销量和评价
	public void setSaleAndReviewNumber(Product product){
		int saleCount = orderItemService.getSaleCount(product);
		product.setSaleCount(saleCount);
		int reviewCount = reviewService.getCount(product);
		product.setReviewCount(reviewCount);
	}
	
	//初始化数据库中的数据到ElasticSearch中
	//先查询ES有没数据，如果没有则把数据同步到ES中
	public void initDatabase2ES(){
		Pageable pageable = new PageRequest(0,5);
		Page<Product> page = productESDAO.findAll(pageable);
		if (page.getContent().isEmpty()){
			List<Product> products = productDAO.findAll();
			for(Product product:products){
				productESDAO.save(product);
			}
		}
	}
		
	//之前是普通的模糊查询，现在通过ProductESDAO到ElasticSearch中进行查询
	public List<Product> search(String keyword, int start, int size){
		initDatabase2ES();
		FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery()
				.add(QueryBuilders.matchPhraseQuery("name", keyword), 
						ScoreFunctionBuilders.weightFactorFunction(100))
				.scoreMode("sum")
				.setMinScore(10);
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		Pageable pageable = new PageRequest(start, size, sort);
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withPageable(pageable)
				.withQuery(functionScoreQueryBuilder).build();
		Page<Product> page = productESDAO.search(searchQuery);
		return page.getContent();
//		Sort sort = new Sort(Sort.Direction.DESC, "id");
//		Pageable pageable = new PageRequest(start, size, sort);
//		List<Product> products = productDAO.findByNameLike("%" + keyword + "%", pageable);
//		return products;
	}
}
