package com.how2java.tmall.web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.how2java.tmall.pojo.Product;
import com.how2java.tmall.pojo.ProductImage;
import com.how2java.tmall.service.ProductImageService;
import com.how2java.tmall.service.ProductService;
import com.how2java.tmall.util.ImageUtil;

@RestController
public class ProductImageController {

	@Autowired
	ProductService productService;
	@Autowired
	ProductImageService productImageService;
	
	@GetMapping("/products/{pid}/productImages")
	public List<ProductImage> list(@RequestParam("type") String type, 
			@PathVariable("pid") int pid) throws Exception{
		Product product = productService.get(pid);
		if (type.equals(ProductImageService.type_single)){
			List<ProductImage> singles = productImageService.listSingleProductImages(product);
			return singles;
		}
		else if (type.equals(ProductImageService.type_detail)){
			List<ProductImage> details = productImageService.listDetailProductImages(product);
			return details;
		}
		else{
			return new ArrayList<>();
		}
	}
	
	@PostMapping("/productImages")
	public Object add(@RequestParam("type") String type, MultipartFile image, 
			@RequestParam("pid") int pid, HttpServletRequest request) throws Exception{
		//数据库的储存
		ProductImage bean = new ProductImage();
		Product product = productService.get(pid);
		bean.setProduct(product);
		bean.setType(type);
		productImageService.add(bean);
		//图片的储存
		String folder = "img/";
		if (type.equals(ProductImageService.type_single)){
			folder += "productSingle";
		}
		else{
			folder += "productDetail";
		}
		File imageFolder = new File(request.getServletContext().getRealPath(folder));
		File file = new File(imageFolder, bean.getId() + ".jpg");
		String fileName = file.getName();
		if (!file.getParentFile().exists()){
			file.getParentFile().mkdirs();
		}
		try{
			//一开始file是空的，现在把image变成这个file，这个时候这个file就是image
			image.transferTo(file);
			//把image转换为jpg格式
			BufferedImage img = ImageUtil.change2jpg(file);
			//真正地把图片写进file里
			ImageIO.write(img, "jpg", file);
		}catch(IOException e){
			e.printStackTrace();
		}
		if (bean.getType().equals(ProductImageService.type_single)){
			String imageFolder_small = request.getServletContext().getRealPath("img/productSingle_small");
			String imageFolder_middle = request.getServletContext().getRealPath("img/productSingle_middle");
			File f_small = new File(imageFolder_small, fileName);
			File f_middle = new File(imageFolder_middle, fileName);
			f_small.getParentFile().mkdirs();
			f_middle.getParentFile().mkdirs();
			ImageUtil.resizeImage(file, 56, 56, f_small);
			ImageUtil.resizeImage(file, 217, 190, f_middle);
		}
		return bean;
	}
	
	@DeleteMapping("/productImages/{id}")
	public String delete(@PathVariable("id") int id, HttpServletRequest request) throws Exception{
		//从数据库中删除
		ProductImage bean = productImageService.get(id);
		productService.delete(id);
		//从文件中删除
		String folder = "img/";
		if (bean.getType().equals(ProductImageService.type_single)){
			folder += "productSingle";
		}
		else{
			folder += "productDetail";
		}
		File imageFolder = new File(request.getServletContext().getRealPath(folder));
		File file = new File(imageFolder, bean.getId() + ".jpg");
		String fileName = file.getName();
		file.delete();
		if (bean.getType().equals(ProductImageService.type_single)){
			String imageFolder_small = request.getServletContext().getRealPath("img/productSingle_small");
			String imageFolder_middle = request.getServletContext().getRealPath("img/productSingle_middle");
			File f_small = new File(imageFolder_small, fileName);
			File f_middle = new File(imageFolder_middle, fileName);
			f_small.delete();
			f_middle.delete();
		}
		return null;
	}
	
}
