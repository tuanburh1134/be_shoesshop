package com.shoes.ecommerce.service;

import com.shoes.ecommerce.entity.OrderEntity;
import com.shoes.ecommerce.entity.OrderItem;
import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.repository.OrderRepository;
import com.shoes.ecommerce.repository.UserRepository;
import com.shoes.ecommerce.repository.ProductRepository;
import com.shoes.ecommerce.entity.Product;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, ProductRepository productRepository){
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public OrderEntity createOrder(OrderEntity order, String username){
        if(username != null){
            User u = userRepository.findByUsername(username).orElse(null);
            order.setUser(u);
        }
        order.setStatus(order.getStatus() == null ? "pending" : order.getStatus());
        order.setCreatedAt(System.currentTimeMillis());
        if(order.getMethod() != null && (order.getMethod().equalsIgnoreCase("bank_transfer") || order.getMethod().equalsIgnoreCase("payos") || order.getMethod().equalsIgnoreCase("account"))){
            order.setPaymentStatus("unpaid");
        }else{
            order.setPaymentStatus(order.getPaymentStatus() == null ? "unpaid" : order.getPaymentStatus());
        }
        if(order.getItems() != null){
            // validate and decrement inventory for each ordered item
            for(OrderItem it: order.getItems()){
                it.setOrder(order);
                if(it.getProductId() != null){
                    Product p = productRepository.findById(it.getProductId()).orElse(null);
                    if(p != null && it.getQty() != null && it.getQty() > 0){
                        // try to decrement size-specific qty fields if available
                        try{
                            String size = it.getSize();
                            int need = it.getQty();
                            boolean decremented = false;
                            if(size != null){
                                switch(size.trim()){
                                    case "39": if(p.getQty39() != null && p.getQty39() >= need){ p.setQty39(p.getQty39() - need); decremented = true; } break;
                                    case "40": if(p.getQty40() != null && p.getQty40() >= need){ p.setQty40(p.getQty40() - need); decremented = true; } break;
                                    case "41": if(p.getQty41() != null && p.getQty41() >= need){ p.setQty41(p.getQty41() - need); decremented = true; } break;
                                    case "42": if(p.getQty42() != null && p.getQty42() >= need){ p.setQty42(p.getQty42() - need); decremented = true; } break;
                                    case "43": if(p.getQty43() != null && p.getQty43() >= need){ p.setQty43(p.getQty43() - need); decremented = true; } break;
                                    case "44": if(p.getQty44() != null && p.getQty44() >= need){ p.setQty44(p.getQty44() - need); decremented = true; } break;
                                }
                            }
                            if(!decremented){
                                // if we didn't decrement by size, try to decrement overall inventory if possible
                                // attempt to decrement any non-null qty field
                                Integer[] arr = new Integer[]{p.getQty39(),p.getQty40(),p.getQty41(),p.getQty42(),p.getQty43(),p.getQty44()};
                                for(int i=0;i<arr.length;i++){
                                    if(arr[i] != null && arr[i] >= need){
                                        switch(i){
                                            case 0: p.setQty39(arr[i]-need); break;
                                            case 1: p.setQty40(arr[i]-need); break;
                                            case 2: p.setQty41(arr[i]-need); break;
                                            case 3: p.setQty42(arr[i]-need); break;
                                            case 4: p.setQty43(arr[i]-need); break;
                                            case 5: p.setQty44(arr[i]-need); break;
                                        }
                                        decremented = true; break;
                                    }
                                }
                            }
                            if(!decremented){
                                throw new IllegalArgumentException("Sản phẩm '" + it.getName() + "' (size " + it.getSize() + ") không đủ hàng");
                            }
                            // save updated product
                            productRepository.save(p);
                        }catch(IllegalArgumentException ex){ throw ex; }
                        catch(Exception ex){ /* ignore product update failures */ }
                    }
                }
            }
        }
        return orderRepository.save(order);
    }

    public List<OrderEntity> listAll(){ return orderRepository.findAll(); }
    public List<OrderEntity> listByUsername(String username){
        if(username == null) return List.of();
        var u = userRepository.findByUsername(username).orElse(null);
        if(u == null) return List.of();
        return orderRepository.findByUserId(u.getId());
    }
    public Optional<OrderEntity> findById(Long id){ return orderRepository.findById(id); }
    public List<OrderEntity> findByUserId(Long id){ return orderRepository.findByUserId(id); }
    public OrderEntity save(OrderEntity o){ return orderRepository.save(o); }
}
