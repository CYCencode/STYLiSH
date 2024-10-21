package com.example.stylish.controller;

import com.example.stylish.model.Product;
import com.example.stylish.service.ProductService;
import com.example.stylish.service.ProductSupplier;
import com.example.stylish.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/1.0/products")
public class ProductController {

    private final ProductService productService;
    private final S3Service s3Service;

    @Autowired
    public ProductController(ProductService productService, S3Service s3Service) {
        this.productService = productService;
        this.s3Service = s3Service;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(@RequestParam String keyword, @RequestParam(required = false) String paging) {
        return getProductResponse(() -> productService.getProductByTitle(keyword, getPage(paging)));
    }

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> searchProductDetails(@RequestParam int id) {
        return getProductResponse(() -> productService.getProductById(id));
    }

    @GetMapping("/accessories")
    public ResponseEntity<Map<String, Object>> accProducts(@RequestParam(required = false) String paging) {
        return getProductResponse(() -> productService.getAccProduct(getPage(paging)));
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> allProducts(@RequestParam(required = false) String paging) {
        return getProductResponse(() -> productService.getAllProduct(getPage(paging)));
    }

    @GetMapping("/women")
    public ResponseEntity<Map<String, Object>> womenProducts(@RequestParam(required = false) String paging) {
        return getProductResponse(() -> productService.getWomenProduct(getPage(paging)));
    }

    @GetMapping("/men")
    public ResponseEntity<Map<String, Object>> menProducts(@RequestParam(required = false) String paging) {
        return getProductResponse(() -> productService.getMenProduct(getPage(paging)));
    }

    private ResponseEntity<Map<String, Object>> getProductResponse(ProductSupplier productSupplier) {
        try {
            Map<String, Object> products = productSupplier.get();
            // 更新產品圖片的URL
            updateImageUrls(products);
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private int getPage(String paging) {
        return (paging != null) ? Integer.parseInt(paging) : 0;
    }

    private void updateImageUrls(Map<String, Object> products) {
        if (products.containsKey("data")) {
            @SuppressWarnings("unchecked")
            List<Product> productList = (List<Product>) products.get("data");
            productList.forEach(product -> {
                // 更新主圖片 URL
                String mainImageUrl = s3Service.getFileUrl(product.getMainImage());
                product.setMainImage(mainImageUrl);

                // 更新其他圖片 URL
                List<String> images = product.getImages();
                List<String> updatedImages = new ArrayList<>();
                images.forEach(img -> updatedImages.add(s3Service.getFileUrl(img)));
                product.setImages(updatedImages);
            });
        }
    }
}
