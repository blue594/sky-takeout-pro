package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入订单明细数据
     * @param orderDetails
     */
    void insertBatch(List<OrderDetail> orderDetails);

    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);

    /**
     * 根据用户ID和订单状态查询订单详情
     */
    @Select("select od.* from order_detail od " +
            "left join orders o on od.order_id = o.id " +
            "where o.user_id = #{userId} and o.status = #{status}")
    List<OrderDetail> listByUserIdAndStatus(Long userId, Integer status);

    /**
     * 统计指定状态订单中的菜品销量（按销量倒序）
     */
    List<Map<String, Object>> statisticsDishSales(
            @Param("status") Integer status,
            @Param("limit") Integer limit
    );
}