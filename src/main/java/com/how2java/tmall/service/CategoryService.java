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

import com.how2java.tmall.dao.CategoryDAO;
import com.how2java.tmall.pojo.Category;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.util.Page4Navigator;

@Service
@CacheConfig(cacheNames="categories")
public class CategoryService {

	@Autowired
	CategoryDAO categoryDAO;
	
	@Cacheable(key="'categories-all'")
	public List<Category> list(){
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		return categoryDAO.findAll(sort);
	}
	
	@Cacheable(key="'categories-page-'+ #p0 + '-'+ #p1")
	public Page4Navigator<Category> list(int start, int size, int navigatePages){
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		Pageable pageable = new PageRequest(start, size, sort);
		Page pageFromJPA = categoryDAO.findAll(pageable);
		return new Page4Navigator<>(pageFromJPA, navigatePages);
	}
	
	//不用@CachePut(key="'category-one-'+ #p0")是因为它虽然的确可以在redis中增加一条数据，
	//但不能更新分页缓存中categories-page-0-5及之类的缓存数据，导致出现数据不一致的问题
	//所以解决方法是一旦增加某个数据，就把缓存所有分类相关的数据都清除掉，使得下一次访问需重新创建缓存
	//这样，牺牲了一点点性能，但是保障了数据的一致性
	//以下的delete和update方法同理
	@CacheEvict(allEntries=true)
	public void add(Category bean){
		categoryDAO.save(bean);
	}
	
	@CacheEvict(allEntries=true)
	public void delete(int id){
		categoryDAO.delete(id);
	}
	
	@Cacheable(key="'categories-one-'+ #p0")
	public Category get(int id){
		Category c = categoryDAO.findOne(id);
		return c;
	}
	
	@CacheEvict(allEntries=true)
	public void update(Category bean){
		categoryDAO.save(bean);
	}
	
	public void removeCategoryFromProduct(List<Category> cs){
		for (Category c:cs){
			removeCategoryFromProduct(c);
		}
	}
	//把Product的Category属性移除
	//因为在对分类做序列还转换为 json 的时候，会遍历里面的 products, 然后遍历出来的产品上，又会有分类，导致依赖循环
    //而在这里去掉，就没事了。 只要在前端业务上，没有通过产品获取分类的业务，去掉也没有关系
	public void removeCategoryFromProduct(Category category){
		List<Product> products = category.getProducts();
		if (products != null){
			for (Product product:products){
				product.setCategory(null);
			}
		}
		
		List<List<Product>> productsByRow = category.getProductsByRow();
		if (productsByRow != null){
			for (List<Product> ps:productsByRow){
				for (Product p:ps){
					p.setCategory(null);
				}
			}
		}
	}
}
