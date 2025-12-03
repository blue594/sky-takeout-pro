package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {
    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    @Delete("delete from dish where id =#{id}")
    void deleteById(Long id);

    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    List<Dish> list(Dish dish);

    List<Dish> getBySetmealId(Long id);

    List<Integer> selectCategoryIdsByDishIds(List<Long> ids);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    // 在DishMapper.java中添加查询月销量的方法
    @Select("SELECT sum(od.number) FROM order_detail od " +
            "INNER JOIN orders o ON od.order_id = o.id " +
            "WHERE od.dish_id = #{dishId} " +
            "AND o.status = 5 " + // 已完成订单
            "AND o.order_time >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)")
    Integer getMonthSales(Long dishId);
}
