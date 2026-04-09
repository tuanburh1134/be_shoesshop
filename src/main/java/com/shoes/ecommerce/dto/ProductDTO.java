package com.shoes.ecommerce.dto;

import java.util.List;

public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double oldPrice;
    private String brand;
    private boolean hot;
    private String image;
    private String detailImage;
    private List<String> detailImages;
    private String inventory; // JSON string for per-color inventory
    private String detail;
    private Double discount;
    private String size;
    private Integer qty39;
    private Integer qty40;
    private Integer qty41;
    private Integer qty42;
    private Integer qty43;
    private Integer qty44;

    public ProductDTO() {}

    public ProductDTO(Long id, String name, String description, Double price, Double oldPrice, String brand, boolean hot,
                      String image, String detailImage, String detail, Double discount, String size,
                      Integer qty39, Integer qty40, Integer qty41, Integer qty42, Integer qty43, Integer qty44) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.oldPrice = oldPrice;
        this.brand = brand;
        this.hot = hot;
        this.image = image;
        this.detailImage = detailImage;
        this.detail = detail;
        this.discount = discount;
        this.size = size;
        this.qty39 = qty39;
        this.qty40 = qty40;
        this.qty41 = qty41;
        this.qty42 = qty42;
        this.qty43 = qty43;
        this.qty44 = qty44;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getOldPrice() { return oldPrice; }
    public void setOldPrice(Double oldPrice) { this.oldPrice = oldPrice; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public boolean isHot() { return hot; }
    public void setHot(boolean hot) { this.hot = hot; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getDetailImage() { return detailImage; }
    public void setDetailImage(String detailImage) { this.detailImage = detailImage; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getQty39() { return qty39; }
    public void setQty39(Integer qty39) { this.qty39 = qty39; }
    public Integer getQty40() { return qty40; }
    public void setQty40(Integer qty40) { this.qty40 = qty40; }
    public Integer getQty41() { return qty41; }
    public void setQty41(Integer qty41) { this.qty41 = qty41; }
    public Integer getQty42() { return qty42; }
    public void setQty42(Integer qty42) { this.qty42 = qty42; }
    public Integer getQty43() { return qty43; }
    public void setQty43(Integer qty43) { this.qty43 = qty43; }
    public Integer getQty44() { return qty44; }
    public void setQty44(Integer qty44) { this.qty44 = qty44; }
    public List<String> getDetailImages() { return detailImages; }
    public void setDetailImages(List<String> detailImages) { this.detailImages = detailImages; }
    public String getInventory() { return inventory; }
    public void setInventory(String inventory) { this.inventory = inventory; }
}
