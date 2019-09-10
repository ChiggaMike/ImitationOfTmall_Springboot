package com.how2java.tmall.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.how2java.tmall.dao.PropertyValueDAO;
import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.Property;
import com.how2java.tmall.pojo.PropertyValue;

@Service
@CacheConfig(cacheNames="propertyValues")
public class PropertyValueService {

	@Autowired
	PropertyValueDAO propertyValueDAO;
	@Autowired
	PropertyService propertyService;
	
	//init是因为PropertyValue没有增加，只有修改
	//用属性的id和产品的id去查询，看看这个属性和这个产品，是否已经存在属性值
	//如果不存在，就创建一个属性值，并设置其属性和产品，接着插入到数据库中
	public void init(Product product){
		List<Property> properties = propertyService.listByCategory(product.getCategory());
		for (Property property:properties){
			PropertyValue propertyValue = getByPropertyAndProduct(product, property);
			if (propertyValue == null){
				propertyValue = new PropertyValue();
				propertyValue.setProduct(product);
				propertyValue.setProperty(property);
				propertyValueDAO.save(propertyValue);
			}
		}
	}
	
	@Cacheable(key="'propertyValues-one-pid-'+ #p0.id+ '-ptid-'+ #p1.id")
	public PropertyValue getByPropertyAndProduct(Product product, Property property){
		return propertyValueDAO.getByPropertyAndProduct(property, product);
	}
	
	@CacheEvict(allEntries=true)
	public void update(PropertyValue bean){
		propertyValueDAO.save(bean);
	}
	
	@Cacheable(key="'propertyValues-pid-'+ #p0.id")
	public List<PropertyValue> list(Product product){
		return propertyValueDAO.findByProductOrderByIdDesc(product);
	}
}
