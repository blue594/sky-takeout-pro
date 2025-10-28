package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ?") //每分钟处理一次支付超时订单
    public void processTimeoutOrder(){
        log.info("处理支付超时订单:{}",new Date());
        //获取到当前时间的十五分钟前这个时间点
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        //获取到Time这个时间节点及之前的所有未支付订单
        List<Orders> orderList = orderMapper.getByStatusAndOrdertimeLT(Orders.PENDING_PAYMENT, time);
        //将订单状态设置为已取消，并设置取消原因和取消时间
        if(orderList != null && orderList.size() > 0){
            orderList.forEach(orders -> {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("用户超时未支付");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            });
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨一点处理一次“派送中”状态的订单
    public void processDeliveryOrder(){
        log.info("处理派送中订单:{}",new Date());
        //获取到当前时间的一个小时前这个时间节点(该项目设置一小时内派送成功)
        LocalDateTime time = LocalDateTime.now().minusHours(1);
        //获取到该时间节点下所有派送中的订单
        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        //修改订单状态为已完成
        if(ordersList != null && ordersList.size() > 0){
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            });
        }
    }
}
