package com.sky.service.impl;

import com.sky.entity.GuessYouLike;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.GuessYouLikeMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.service.DishService;
import com.sky.service.GuessYouLikeService;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GuessYouLikeServiceImpl implements GuessYouLikeService {

    @Autowired
    private GuessYouLikeMapper guessYouLikeMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    // 新增依赖注入，用于复用已有方法
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * 检查并初始化用户的猜你喜欢数据
     */
    @Override
    @Transactional
    public void initOrUpdateGuessYouLike(Long userId) {
        // 1. 检查用户是否已有猜你喜欢数据，有则直接返回
        Integer count = guessYouLikeMapper.countByUserId(userId);
        if (count > 0) {
            return;
        }

        // 2. 检查用户是否下单过（只统计已完成的订单）
        List<OrderDetail> orderDetails = orderDetailMapper.listByUserIdAndStatus(userId, Orders.COMPLETED);
        boolean hasOrdered = orderDetails != null && !orderDetails.isEmpty();

        List<GuessYouLike> guessList = new ArrayList<>();
        int score = 1;

        if (hasOrdered) {
            // 2.1 下单过：推荐下单前三的菜品 + 前二的套餐 + 店铺销量前五的菜品
            // 2.1.1 统计用户下单菜品次数，取前三
            Map<Long, Long> dishCountMap = orderDetails.stream()
                    .filter(detail -> detail.getDishId() != null)
                    .collect(Collectors.groupingBy(OrderDetail::getDishId, Collectors.counting()));
            List<Long> top3DishIds = getTopIds(dishCountMap, 3);

            // 2.1.2 统计用户下单套餐次数，取前二
            Map<Long, Long> setmealCountMap = orderDetails.stream()
                    .filter(detail -> detail.getSetmealId() != null)
                    .collect(Collectors.groupingBy(OrderDetail::getSetmealId, Collectors.counting()));
            List<Long> top2SetmealIds = getTopIds(setmealCountMap, 2);

            // 2.1.3 店铺销量前五的菜品（从订单详情表统计已完成订单的销量）
            List<Long> top5SaleDishIds = getTopSaleDishIds(5);

            // 合并数据并去重（避免重复推荐同一菜品）
            Set<Long> addedDishIds = new HashSet<>(top3DishIds);
            top5SaleDishIds.forEach(id -> {
                if (!addedDishIds.contains(id)) {
                    top3DishIds.add(id);
                }
            });

            // 转换为GuessYouLike对象
            for (Long dishId : top3DishIds) {
                guessList.add(buildGuessYouLike(userId, dishId, null, score++));
            }
            for (Long setmealId : top2SetmealIds) {
                guessList.add(buildGuessYouLike(userId, null, setmealId, score++));
            }
        } else {
            // 2.2 未下单过：推荐店铺销量前十的菜品（从订单详情表统计）
            List<Long> top10SaleDishIds = getTopSaleDishIds(10);
            for (Long dishId : top10SaleDishIds) {
                guessList.add(buildGuessYouLike(userId,  dishId, null, score++));
            }
        }

        // 3. 批量插入数据
        if (!guessList.isEmpty()) {
            guessYouLikeMapper.insertBatch(guessList);
        }
    }

    /**
     * 查询用户的猜你喜欢数据
     */
    @Override
    public List<Object> getGuessYouLikeData(Long userId) {
        List<GuessYouLike> guessList = guessYouLikeMapper.listByUserId(userId);
        if (guessList.isEmpty()) {
            return Collections.emptyList();
        }

        // 分离菜品和套餐ID，并限制数量（前3菜品+前2套餐）
        List<Long> dishIds = guessList.stream()
                .filter(g -> g.getDishId() != null)
                .map(GuessYouLike::getDishId)
                .distinct()
                .limit(3)
                .collect(Collectors.toList());

        List<Long> setmealIds = guessList.stream()
                .filter(g -> g.getSetmealId() != null)
                .map(GuessYouLike::getSetmealId)
                .distinct()
                .limit(2)
                .collect(Collectors.toList());

        // 查询带口味的菜品
        List<DishVO> dishes = dishIds.stream()
                .map(dishService::getByIdWithFlavors)
                .collect(Collectors.toList());

        // 查询套餐并转换为与DishVO兼容的结构
        List<SetmealVO> setmeals = setmealIds.stream()
                .map(setmealService::getByIdWithDish)
                .collect(Collectors.toList());

        // 合并菜品和套餐为统一列表（前端可通过类型区分）
        List<Object> result = new ArrayList<>();
        result.addAll(dishes);
        result.addAll(setmeals);

        return result;
    }

    /**
     * 从订单详情表统计销量前N的菜品ID（只统计已完成订单）
     */
    private List<Long> getTopSaleDishIds(int limit) {
        // 查询已完成订单中的菜品销量，按销量倒序
        List<Map<String, Object>> saleList = orderDetailMapper.statisticsDishSales(Orders.COMPLETED);

        // 提取菜品ID并按销量排序取前N
        return saleList.stream()
                .map(item -> Long.valueOf(item.get("dish_id").toString()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // 工具方法：构建GuessYouLike对象（区分菜品和套餐）
    private GuessYouLike buildGuessYouLike(Long userId, Long dishId, Long setmealId, Integer score) {
        GuessYouLike guess = new GuessYouLike();
        guess.setUserId(userId);
        guess.setDishId(dishId);
        guess.setSetmealId(setmealId);
        guess.setScore(score);
        guess.setCreateTime(LocalDateTime.now());
        return guess;
    }


    // 工具方法：从统计map中取topN的id
    private List<Long> getTopIds(Map<Long, Long> countMap, int limit) {
        return countMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}