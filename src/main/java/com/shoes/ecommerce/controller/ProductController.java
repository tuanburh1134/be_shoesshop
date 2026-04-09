package com.shoes.ecommerce.controller;

import com.shoes.ecommerce.dto.ProductDTO;
import com.shoes.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.http.MediaType;
import java.security.Principal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import java.util.Base64;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final Logger logger = LoggerFactory.getLogger(ProductController.class);

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> list() {
        logger.debug("GET /api/products called");
        return ResponseEntity.ok(productService.listAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDTO> getOne(@PathVariable Long id) {
        logger.debug("GET /api/products/{} called", id);
        ProductDTO dto = productService.getById(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDTO> create(@RequestBody ProductDTO dto, Principal principal) {
        logger.info("POST /api/products by {}", principal == null ? "anonymous" : principal.getName());
        logger.debug("Payload: {}", dto);
        ProductDTO created = productService.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> createWithFiles(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false, name = "oldPrice") Double oldPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String detail,
            @RequestParam(required = false) Double discount,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Integer qty39,
            @RequestParam(required = false) Integer qty40,
            @RequestParam(required = false) Integer qty41,
            @RequestParam(required = false) Integer qty42,
            @RequestParam(required = false) Integer qty43,
            @RequestParam(required = false) Integer qty44,
            @RequestPart(required = false) MultipartFile image,
            @RequestPart(required = false, name = "detailImage") MultipartFile detailImage,
            @RequestPart(required = false, name = "detailImages") MultipartFile[] detailImages,
            @RequestParam(required = false) String inventory,
            Principal principal) {

        logger.info("POST /api/products/upload by {}", principal == null ? "anonymous" : principal.getName());
        ProductDTO dto = new ProductDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setPrice(price);
        dto.setOldPrice(oldPrice);
        dto.setBrand(brand);
        dto.setDetail(detail);
        dto.setDiscount(discount);
        dto.setSize(size);
        dto.setQty39(qty39);
        dto.setQty40(qty40);
        dto.setQty41(qty41);
        dto.setQty42(qty42);
        dto.setQty43(qty43);
        dto.setQty44(qty44);

        try {
            if (image != null && !image.isEmpty()) {
                dto.setImage(toDataUrl(image));
            }
            // single detailImage (backward compatible)
            if (detailImage != null && !detailImage.isEmpty()) {
                dto.setDetailImage(toDataUrl(detailImage));
            }
            // multiple detail images
            if (detailImages != null && detailImages.length > 0) {
                java.util.List<String> saved = new java.util.ArrayList<>();
                int count = 0;
                for (MultipartFile f : detailImages) {
                    if (f == null || f.isEmpty()) continue;
                    if (count >= 10) break; // limit to 10
                    saved.add(toDataUrl(f));
                    count++;
                }
                dto.setDetailImages(saved);
            }
            if(inventory != null) dto.setInventory(inventory);
        } catch (IOException ex) {
            logger.error("Failed to process uploaded files", ex);
            return ResponseEntity.status(500).build();
        }

        ProductDTO created = productService.create(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping(value = "{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDTO> update(@PathVariable Long id, @RequestBody ProductDTO dto, Principal principal) {
        logger.info("PUT /api/products/{} by {}", id, principal == null ? "anonymous" : principal.getName());
        logger.debug("Payload: {}", dto);
        ProductDTO updated = productService.update(id, dto);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @PutMapping(value = "{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> updateWithFiles(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false, name = "oldPrice") Double oldPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String detail,
            @RequestParam(required = false) Double discount,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Integer qty39,
            @RequestParam(required = false) Integer qty40,
            @RequestParam(required = false) Integer qty41,
            @RequestParam(required = false) Integer qty42,
            @RequestParam(required = false) Integer qty43,
            @RequestParam(required = false) Integer qty44,
            @RequestPart(required = false) MultipartFile image,
            @RequestPart(required = false, name = "detailImage") MultipartFile detailImage,
            @RequestPart(required = false, name = "detailImages") MultipartFile[] detailImages,
            @RequestParam(required = false) String inventory,
            Principal principal) {

        logger.info("PUT /api/products/{}/upload by {}", id, principal == null ? "anonymous" : principal.getName());
        ProductDTO dto = new ProductDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setPrice(price);
        dto.setOldPrice(oldPrice);
        dto.setBrand(brand);
        dto.setDetail(detail);
        dto.setDiscount(discount);
        dto.setSize(size);
        dto.setQty39(qty39);
        dto.setQty40(qty40);
        dto.setQty41(qty41);
        dto.setQty42(qty42);
        dto.setQty43(qty43);
        dto.setQty44(qty44);

        try {
            if (image != null && !image.isEmpty()) {
                dto.setImage(toDataUrl(image));
            }
            if (detailImage != null && !detailImage.isEmpty()) {
                dto.setDetailImage(toDataUrl(detailImage));
            }
            if (detailImages != null && detailImages.length > 0) {
                java.util.List<String> saved = new java.util.ArrayList<>();
                int count = 0;
                for (MultipartFile f : detailImages) {
                    if (f == null || f.isEmpty()) continue;
                    if (count >= 10) break;
                    saved.add(toDataUrl(f));
                    count++;
                }
                dto.setDetailImages(saved);
            }
            if(inventory != null) dto.setInventory(inventory);
        } catch (IOException ex) {
            logger.error("Failed to process uploaded files", ex);
            return ResponseEntity.status(500).build();
        }

        ProductDTO updated = productService.update(id, dto);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        logger.info("DELETE /api/products/{} by {}", id, principal == null ? "anonymous" : principal.getName());
        boolean ok = productService.delete(id);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    private String toDataUrl(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        return "data:" + contentType + ";base64," + base64;
    }
}
