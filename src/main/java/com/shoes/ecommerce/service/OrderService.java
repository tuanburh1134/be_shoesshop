package com.shoes.ecommerce.service;

import com.shoes.ecommerce.entity.OrderEntity;
import com.shoes.ecommerce.entity.OrderItem;
import com.shoes.ecommerce.entity.User;
import com.shoes.ecommerce.repository.OrderRepository;
import com.shoes.ecommerce.repository.UserRepository;
import com.shoes.ecommerce.repository.ProductRepository;
import com.shoes.ecommerce.entity.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                        // Decrement inventory at order creation time (status pending) and persist in DB.
                        try{
                            int need = it.getQty();
                            String size = it.getSize();
                            boolean decremented = false;

                            // New flow: decrement by color-size in JSON inventory if available.
                            if(tryDecrementFromInventory(p, it.getColor(), size, need)){
                                decremented = true;
                            }

                            // Fallback to legacy size fields (qty39..qty44)
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

    public OrderEntity cancelPendingOrderByUser(Long orderId, String username, String reason){
        if(orderId == null) throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        if(username == null || username.isBlank()) throw new IllegalArgumentException("Bạn cần đăng nhập để hủy đơn");

        var opt = orderRepository.findById(orderId);
        if(opt.isEmpty()) throw new IllegalArgumentException("Không tìm thấy đơn hàng");
        OrderEntity order = opt.get();

        if(order.getUser() == null || order.getUser().getUsername() == null || !order.getUser().getUsername().equals(username)){
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn này");
        }

        if(!"pending".equalsIgnoreCase(String.valueOf(order.getStatus()))){
            throw new IllegalArgumentException("Chỉ có thể hủy đơn khi đơn chưa được duyệt");
        }

        if(order.getItems() != null){
            for(OrderItem it : order.getItems()){
                if(it.getProductId() == null || it.getQty() == null || it.getQty() <= 0) continue;
                Product p = productRepository.findById(it.getProductId()).orElse(null);
                if(p == null) continue;

                int qty = it.getQty();
                String size = it.getSize();
                boolean restored = false;

                if(tryIncreaseFromInventory(p, it.getColor(), size, qty)){
                    restored = true;
                }

                if(!restored && size != null){
                    switch(size.trim()){
                        case "39": p.setQty39((p.getQty39() == null ? 0 : p.getQty39()) + qty); restored = true; break;
                        case "40": p.setQty40((p.getQty40() == null ? 0 : p.getQty40()) + qty); restored = true; break;
                        case "41": p.setQty41((p.getQty41() == null ? 0 : p.getQty41()) + qty); restored = true; break;
                        case "42": p.setQty42((p.getQty42() == null ? 0 : p.getQty42()) + qty); restored = true; break;
                        case "43": p.setQty43((p.getQty43() == null ? 0 : p.getQty43()) + qty); restored = true; break;
                        case "44": p.setQty44((p.getQty44() == null ? 0 : p.getQty44()) + qty); restored = true; break;
                    }
                }

                if(restored){
                    productRepository.save(p);
                }
            }
        }

        order.setStatus("cancelled");
        order.setApprovedAt(null);
        order.setCancelReason(reason == null || reason.isBlank() ? "Người dùng hủy đơn" : reason);
        return orderRepository.save(order);
    }

    public OrderEntity confirmReceivedByUser(Long orderId, String username){
        if(orderId == null) throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        if(username == null || username.isBlank()) throw new IllegalArgumentException("Bạn cần đăng nhập để xác nhận nhận hàng");

        var opt = orderRepository.findById(orderId);
        if(opt.isEmpty()) throw new IllegalArgumentException("Không tìm thấy đơn hàng");
        OrderEntity order = opt.get();

        if(order.getUser() == null || order.getUser().getUsername() == null || !order.getUser().getUsername().equals(username)){
            throw new IllegalArgumentException("Bạn không có quyền cập nhật đơn này");
        }

        if(!"approved".equalsIgnoreCase(String.valueOf(order.getStatus()))){
            throw new IllegalArgumentException("Chỉ có thể xác nhận nhận hàng khi đơn đang giao");
        }

        order.setStatus("completed");
        return orderRepository.save(order);
    }

    private boolean tryDecrementFromInventory(Product p, String color, String size, int need){
        if(size == null || size.trim().isEmpty()) return false;
        String invRaw = p.getInventory();
        if(invRaw == null || invRaw.isBlank()) return false;

        try{
            Map<String, Map<String, Integer>> inv = objectMapper.readValue(invRaw, new TypeReference<Map<String, Map<String, Integer>>>(){});
            if(inv == null || inv.isEmpty()) return false;

            String sizeKey = size.trim();
            boolean decremented = false;

            if(color != null && !color.trim().isEmpty()){
                String colorKey = findColorKeyIgnoreCase(inv, color.trim());
                if(colorKey != null){
                    Map<String, Integer> colorMap = inv.get(colorKey);
                    Integer current = toInt(colorMap == null ? null : colorMap.get(sizeKey));
                    if(current != null && current >= need){
                        colorMap.put(sizeKey, current - need);
                        decremented = true;
                    }
                }
            } else {
                for(Map<String, Integer> colorMap : inv.values()){
                    if(colorMap == null) continue;
                    Integer current = toInt(colorMap.get(sizeKey));
                    if(current != null && current >= need){
                        colorMap.put(sizeKey, current - need);
                        decremented = true;
                        break;
                    }
                }
            }

            if(!decremented) return false;

            p.setInventory(objectMapper.writeValueAsString(inv));
            syncLegacyQtyFromInventory(p, inv);
            return true;
        }catch(Exception ex){
            return false;
        }
    }

    private boolean tryIncreaseFromInventory(Product p, String color, String size, int qty){
        if(size == null || size.trim().isEmpty()) return false;
        String invRaw = p.getInventory();
        if(invRaw == null || invRaw.isBlank()) return false;
        if(color == null || color.trim().isEmpty()) return false;

        try{
            Map<String, Map<String, Integer>> inv = objectMapper.readValue(invRaw, new TypeReference<Map<String, Map<String, Integer>>>(){});
            if(inv == null) return false;

            String colorKey = findColorKeyIgnoreCase(inv, color.trim());
            if(colorKey == null){
                colorKey = color.trim();
                inv.put(colorKey, new java.util.LinkedHashMap<>());
            }

            Map<String, Integer> colorMap = inv.get(colorKey);
            if(colorMap == null){
                colorMap = new java.util.LinkedHashMap<>();
                inv.put(colorKey, colorMap);
            }

            String sizeKey = size.trim();
            Integer current = toInt(colorMap.get(sizeKey));
            colorMap.put(sizeKey, (current == null ? 0 : current) + qty);

            p.setInventory(objectMapper.writeValueAsString(inv));
            syncLegacyQtyFromInventory(p, inv);
            return true;
        }catch(Exception ex){
            return false;
        }
    }

    private String findColorKeyIgnoreCase(Map<String, ?> map, String color){
        if(map == null || color == null) return null;
        for(String k : map.keySet()){
            if(k != null && k.equalsIgnoreCase(color)) return k;
        }
        return null;
    }

    private Integer toInt(Object val){
        if(val == null) return null;
        if(val instanceof Integer i) return i;
        if(val instanceof Number n) return n.intValue();
        try{ return Integer.parseInt(String.valueOf(val)); }catch(Exception ex){ return null; }
    }

    private void syncLegacyQtyFromInventory(Product p, Map<String, Map<String, Integer>> inv){
        p.setQty39(sumSize(inv, "39"));
        p.setQty40(sumSize(inv, "40"));
        p.setQty41(sumSize(inv, "41"));
        p.setQty42(sumSize(inv, "42"));
        p.setQty43(sumSize(inv, "43"));
        p.setQty44(sumSize(inv, "44"));
    }

    private int sumSize(Map<String, Map<String, Integer>> inv, String size){
        int total = 0;
        if(inv == null) return total;
        for(Map<String, Integer> colorMap : inv.values()){
            if(colorMap == null) continue;
            Integer v = toInt(colorMap.get(size));
            if(v != null && v > 0) total += v;
        }
        return total;
    }
}
