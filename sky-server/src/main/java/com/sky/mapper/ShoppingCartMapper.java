package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Insert("insert into shopping_cart(name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) values " +
            "(#{name},#{image},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{createTime})")
    void insert(ShoppingCart shoppingCart);

    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long currentId);

    @Delete("delete from shopping_cart where dish_id = #{dishId} and user_id = #{userId}")
    void deleteByDishId(Long dishId,Long userId);

    @Delete("delete from shopping_cart where setmeal_id = #{setmealId} and user_id = #{userId}")
    void deleteBySetmealId(Long setmealId,Long userId);

    void insertBatch(List<ShoppingCart> shoppingCartList);
}
