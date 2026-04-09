package com.shoes.ecommerce.service;

import com.shoes.ecommerce.dto.ProductDTO;
import com.shoes.ecommerce.entity.Product;
import com.shoes.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final com.shoes.ecommerce.repository.OrderRepository orderRepository;
    private final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public ProductService(ProductRepository productRepository, com.shoes.ecommerce.repository.OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public List<ProductDTO> listAll() {
        logger.info("Fetching all products");
        List<Product> products = productRepository.findAll();
        // compute sold quantities per product name from orders (exclude cancelled orders)
        java.util.Map<String, Integer> soldByName = new java.util.HashMap<>();
        try{
            var orders = orderRepository.findAll();
            for(var o: orders){
                if(o == null) continue;
                String st = o.getStatus();
                if(st != null && String.valueOf(st).toLowerCase().indexOf("cancel") >= 0) continue; // skip cancelled
                var items = o.getItems();
                if(items == null) continue;
                for(var it: items){
                    if(it == null) continue;
                    String nm = it.getName() == null ? "" : it.getName().trim();
                    int q = it.getQty() == null ? 0 : it.getQty();
                    if(nm.isEmpty() || q <= 0) continue;
                    soldByName.put(nm, soldByName.getOrDefault(nm, 0) + q);
                }
            }
        }catch(Exception ex){ logger.debug("Could not compute sold counts", ex); }
        // determine top-5 best sellers by sold quantity
        java.util.List<java.util.Map.Entry<String,Integer>> sorted = new java.util.ArrayList<>(soldByName.entrySet());
        sorted.sort((a,b)-> Integer.compare(b.getValue(), a.getValue()));
        java.util.Set<String> topNames = new java.util.HashSet<>();
        for(int i=0;i<Math.min(5, sorted.size());i++){ topNames.add(sorted.get(i).getKey()); }
        
        // sort: hot products first (by sold quantity desc), then the rest by name
        java.util.Map<String,Integer> soldMap = soldByName;
        products = products; // keep products variable
        list: {
            // build map from product name to DTO
        }
        // custom sort using soldMap and hot flag in DTO
        java.util.List<ProductDTO> result = new java.util.ArrayList<>();
        result.addAll(products.stream().map(p -> {
            ProductDTO dto = new ProductDTO(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getOldPrice(), p.getBrand(), (p.isHot() || topNames.contains(p.getName())), p.getImage(), p.getDetailImage(), p.getDetail(), p.getDiscount(), p.getSize(), p.getQty39(), p.getQty40(), p.getQty41(), p.getQty42(), p.getQty43(), p.getQty44());
            try{ if(p.getDetailImages() != null){ List<String> imgs = mapper.readValue(p.getDetailImages(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){}); dto.setDetailImages(imgs); } }catch(Exception ex){ }
            dto.setInventory(p.getInventory());
            return dto;
        }).collect(Collectors.toList()));

        result.sort((a,b)->{
            boolean ah = a.isHot(); boolean bh = b.isHot();
            if(ah && !bh) return -1;
            if(!ah && bh) return 1;
            int sa = soldMap.getOrDefault(a.getName(), 0);
            int sb = soldMap.getOrDefault(b.getName(), 0);
            if(sa != sb) return Integer.compare(sb, sa);
            return (a.getName() == null ? "" : a.getName()).compareToIgnoreCase(b.getName() == null ? "" : b.getName());
        });
        return result;
    }

    public ProductDTO create(ProductDTO dto) {
        // determine hot tag automatically for certain brands
        boolean hot = dto.getBrand() != null && (dto.getBrand().equalsIgnoreCase("labubu") || dto.getBrand().equalsIgnoreCase("adidas"));
        Product p = new Product(dto.getName(), dto.getDescription(), dto.getPrice(), dto.getBrand(), hot);
        p.setOldPrice(dto.getOldPrice());
        p.setImage(dto.getImage());
        p.setDetailImage(dto.getDetailImage());
        try{
            if(dto.getDetailImages() != null) p.setDetailImages(mapper.writeValueAsString(dto.getDetailImages()));
        }catch(Exception ex){ logger.debug("Failed to serialize detailImages", ex); }
        p.setInventory(dto.getInventory());
        p.setDetail(dto.getDetail());
        p.setDiscount(dto.getDiscount());
        p.setSize(dto.getSize());
        p.setQty39(dto.getQty39());
        p.setQty40(dto.getQty40());
        p.setQty41(dto.getQty41());
        p.setQty42(dto.getQty42());
        p.setQty43(dto.getQty43());
        p.setQty44(dto.getQty44());
        Product saved = productRepository.save(p);
        ProductDTO out = new ProductDTO(saved.getId(), saved.getName(), saved.getDescription(), saved.getPrice(), saved.getOldPrice(), saved.getBrand(), saved.isHot(),
            saved.getImage(), saved.getDetailImage(), saved.getDetail(), saved.getDiscount(), saved.getSize(),
            saved.getQty39(), saved.getQty40(), saved.getQty41(), saved.getQty42(), saved.getQty43(), saved.getQty44());
        try{
            if(saved.getDetailImages()!=null){ out.setDetailImages(mapper.readValue(saved.getDetailImages(), new TypeReference<List<String>>(){})); }
        }catch(Exception ex){ }
        out.setInventory(saved.getInventory());
        return out;
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) return null;
        Product p = opt.get();
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setOldPrice(dto.getOldPrice());
        p.setBrand(dto.getBrand());
        p.setImage(dto.getImage());
        p.setDetailImage(dto.getDetailImage());
        p.setDetail(dto.getDetail());
        p.setDiscount(dto.getDiscount());
        p.setSize(dto.getSize());
        p.setQty39(dto.getQty39());
        p.setQty40(dto.getQty40());
        p.setQty41(dto.getQty41());
        p.setQty42(dto.getQty42());
        p.setQty43(dto.getQty43());
        p.setQty44(dto.getQty44());
        try{
            if(dto.getDetailImages() != null) p.setDetailImages(mapper.writeValueAsString(dto.getDetailImages()));
        }catch(Exception ex){ logger.debug("Failed to serialize detailImages", ex); }
        p.setInventory(dto.getInventory());
        // recalc hot flag
        boolean hot = dto.getBrand() != null && (dto.getBrand().equalsIgnoreCase("labubu") || dto.getBrand().equalsIgnoreCase("adidas"));
        p.setHot(hot);
        Product saved = productRepository.save(p);
        ProductDTO out = new ProductDTO(saved.getId(), saved.getName(), saved.getDescription(), saved.getPrice(), saved.getOldPrice(), saved.getBrand(), saved.isHot(),
            saved.getImage(), saved.getDetailImage(), saved.getDetail(), saved.getDiscount(), saved.getSize(),
            saved.getQty39(), saved.getQty40(), saved.getQty41(), saved.getQty42(), saved.getQty43(), saved.getQty44());
        try{ if(saved.getDetailImages()!=null) out.setDetailImages(mapper.readValue(saved.getDetailImages(), new TypeReference<List<String>>(){})); }catch(Exception ex){}
        out.setInventory(saved.getInventory());
        return out;
    }

    public ProductDTO getById(Long id) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) return null;
        Product p = opt.get();
        ProductDTO out = new ProductDTO(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getOldPrice(), p.getBrand(), p.isHot(),
            p.getImage(), p.getDetailImage(), p.getDetail(), p.getDiscount(), p.getSize(),
            p.getQty39(), p.getQty40(), p.getQty41(), p.getQty42(), p.getQty43(), p.getQty44());
        try{ if(p.getDetailImages()!=null) out.setDetailImages(mapper.readValue(p.getDetailImages(), new TypeReference<List<String>>(){})); }catch(Exception ex){}
        out.setInventory(p.getInventory());
        return out;
    }

    public boolean delete(Long id) {
        if (!productRepository.existsById(id)) return false;
        productRepository.deleteById(id);
        return true;
    }
}
