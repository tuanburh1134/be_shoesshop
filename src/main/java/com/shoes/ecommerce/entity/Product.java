package com.shoes.ecommerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "old_price")
    private Double oldPrice;

    @Column(length = 100)
    private String brand;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;

    @Lob
    @Column(name = "detail_image", columnDefinition = "LONGTEXT")
    private String detailImage;

    @Lob
    @Column(name = "detail_images", columnDefinition = "LONGTEXT")
    private String detailImages; // JSON array string of detail image URLs

    @Column(name = "inventory", columnDefinition = "TEXT")
    private String inventory; // JSON structure: {"white":{"39":1,...},"black":{...}}

    @Column(length = 2000)
    private String detail;

    private Double discount;

    @Column(length = 200)
    private String size;

    @Column(name = "qty_39")
    private Integer qty39;
    @Column(name = "qty_40")
    private Integer qty40;
    @Column(name = "qty_41")
    private Integer qty41;
    @Column(name = "qty_42")
    private Integer qty42;
    @Column(name = "qty_43")
    private Integer qty43;
    @Column(name = "qty_44")
    private Integer qty44;

    @Column(nullable = false)
    private boolean hot = false;

    public Product() {}

    public Product(String name, String description, Double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public Product(String name, String description, Double price, String brand, boolean hot) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.brand = brand;
        this.hot = hot;
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
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getDetailImage() { return detailImage; }
    public void setDetailImage(String detailImage) { this.detailImage = detailImage; }
    public String getDetailImages() { return detailImages; }
    public void setDetailImages(String detailImages) { this.detailImages = detailImages; }
    public String getInventory() { return inventory; }
    public void setInventory(String inventory) { this.inventory = inventory; }
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
    public boolean isHot() { return hot; }
    public void setHot(boolean hot) { this.hot = hot; }
}
