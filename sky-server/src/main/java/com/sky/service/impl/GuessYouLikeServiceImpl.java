package com.sky.service.impl;

import com.sky.entity.GuessYouLike;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.GuessYouLikeMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.SetmealMapper;
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
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 检查并初始化用户的猜你喜欢数据
     */
    @Override
    @Transactional
    public void initOrUpdateGuessYouLike(Long userId) {
        // 1. 删除用户旧数据
        guessYouLikeMapper.deleteByUserId(userId);

        // 2. 查询用户已完成订单明细
        List<OrderDetail> orderDetails = orderDetailMapper.listByUserIdAndStatus(userId, Orders.COMPLETED);
        boolean hasOrdered = orderDetails != null && !orderDetails.isEmpty();

        List<GuessYouLike> guessList = new ArrayList<>();

        if (hasOrdered) {
            // 3. 统计用户Top3菜品（按购买次数倒序）
            Map<Long, Long> dishCountMap = orderDetails.stream()
                    .filter(detail -> detail.getDishId() != null)
                    .collect(Collectors.groupingBy(OrderDetail::getDishId, Collectors.counting()));

            List<Long> top3DishIds = dishCountMap.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int countCompare = e2.getValue().compareTo(e1.getValue());
                        return countCompare != 0 ? countCompare : e2.getKey().compareTo(e1.getKey());
                    })
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 4. 统计用户Top2套餐（按购买次数倒序）
            Map<Long, Long> setmealCountMap = orderDetails.stream()
                    .filter(detail -> detail.getSetmealId() != null)
                    .collect(Collectors.groupingBy(OrderDetail::getSetmealId, Collectors.counting()));

            List<Long> top2SetmealIds = setmealCountMap.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int countCompare = e2.getValue().compareTo(e1.getValue());
                        return countCompare != 0 ? countCompare : e2.getKey().compareTo(e1.getKey());
                    })
                    .limit(2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 5. 获取店铺销量Top5菜品（去重用户已购菜品）
            List<Long> top5SaleDishIds = getTopSaleDishIds(5);
            Set<Long> userDishSet = new HashSet<>(top3DishIds); // 用户已购菜品去重集合
            List<Long> shopTop5DishIds = top5SaleDishIds.stream()
                    .filter(id -> !userDishSet.contains(id)) // 排除用户已购买的菜品
                    .collect(Collectors.toList());

            // 6. 构建推荐列表（用户菜品+用户套餐+店铺菜品，保持各自排序）
            int score = 1;
            // 添加用户Top3菜品
            for (Long dishId : top3DishIds) {
                guessList.add(buildGuessYouLike(userId, dishId, null, score++));
            }
            // 添加用户Top2套餐
            for (Long setmealId : top2SetmealIds) {
                guessList.add(buildGuessYouLike(userId, null, setmealId, score++));
            }
            // 添加店铺Top5去重菜品
            for (Long dishId : shopTop5DishIds) {
                guessList.add(buildGuessYouLike(userId, dishId, null, score++));
            }
        } else {
            // 未下单用户：直接取店铺销量Top5菜品
            List<Long> top5SaleDishIds = getTopSaleDishIds(5);
            int score = 1;
            for (Long dishId : top5SaleDishIds) {
                guessList.add(buildGuessYouLike(userId, dishId, null, score++));
            }
        }

        // 7. 批量插入新数据
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

        // 分离菜品和套餐ID
        List<Long> dishIds = guessList.stream()
                .filter(g -> g.getDishId() != null)
                .map(GuessYouLike::getDishId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> setmealIds = guessList.stream()
                .filter(g -> g.getSetmealId() != null)
                .map(GuessYouLike::getSetmealId)
                .distinct()
                .limit(2)
                .collect(Collectors.toList());

        // 查询带口味和月销量的菜品
        List<DishVO> dishes = dishIds.stream()
                .map(dishId -> {
                    DishVO dishVO = dishService.getByIdWithFlavors(dishId);
                    // 补充月销量
                    Integer monthSales = dishMapper.getMonthSales(dishId);
                    dishVO.setMonthSales(monthSales != null ? monthSales : 0);
                    return dishVO;
                })
                .collect(Collectors.toList());

        // 查询套餐并补充月销量
        List<SetmealVO> setmeals = setmealIds.stream()
                .map(setmealId -> {
                    SetmealVO setmealVO = setmealService.getByIdWithDish(setmealId);
                    // 补充套餐月销量（需新增SetmealMapper的getMonthSales方法）
                    Integer monthSales = setmealMapper.getMonthSales(setmealId);
                    setmealVO.setMonthSales(monthSales != null ? monthSales : 0);
                    return setmealVO;
                })
                .collect(Collectors.toList());

        // 合并结果
        List<Object> result = new ArrayList<>();
        result.addAll(dishes);
        result.addAll(setmeals);
        return result;
    }

    /**
     * 从订单详情表统计销量前N的菜品ID（只统计已完成订单）
     */
    private List<Long> getTopSaleDishIds(int limit) {
        List<Map<String, Object>> saleList = orderDetailMapper.statisticsDishSales(
                Orders.COMPLETED,  // 订单状态：已完成
                limit              // 数量限制
        );

        // 提取菜品ID并处理空值
        return saleList.stream()
                .filter(item -> item.get("dish_id") != null)
                .map(item -> Long.valueOf(item.get("dish_id").toString()))
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