package com.sky.controller.user;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import com.sky.service.DishService;
import com.sky.service.GuessYouLikeService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private GuessYouLikeService guessYouLikeService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 根据分类查询菜品（包含猜你喜欢的特殊处理）
     * @param categoryId 分类id
     * @return 菜品列表或猜你喜欢数据
     */
    @GetMapping("/list")
    @ApiOperation("根据分类查询菜品")
    public Result<?> list(Long categoryId) {
        // 1. 先查询分类名称，判断是否为"猜你喜欢"分类
        Category category = categoryService.getById(categoryId);
        boolean isGuessYouLike = category != null && MessageConstant.MIGHT_LIKE.equals(category.getName());

        // 2. 构建Redis缓存键（基于是否为猜你喜欢分类区分）
        String key;
        if (isGuessYouLike) {
            // 猜你喜欢缓存键：加入用户ID确保数据隔离
            key = "dish_guess_you_like_" + BaseContext.getCurrentId();
        } else {
            // 普通分类缓存键：使用分类ID
            key = "dish_" + categoryId;
        }

        // 3. 先从Redis查询
        Object cacheData = redisTemplate.opsForValue().get(key);
        if (cacheData != null) {
            return Result.success(cacheData);
        }

        // 4. Redis无数据，查询数据库
        if (isGuessYouLike) {
            // 处理猜你喜欢逻辑
            Long userId = BaseContext.getCurrentId();
            guessYouLikeService.initOrUpdateGuessYouLike(userId);
            List<Object> guessData = guessYouLikeService.getGuessYouLikeData(userId);
            redisTemplate.opsForValue().set(key, guessData);
            return Result.success(guessData);
        } else {
            // 处理普通菜品查询逻辑
            if (categoryId == null) {
                return Result.error("分类ID不能为空");
            }

            Dish dish = new Dish();
            dish.setCategoryId(categoryId);
            dish.setStatus(StatusConstant.ENABLE); // 只查询起售中的菜品
            List<DishVO> list = dishService.listWithFlavor(dish);

            // 存入Redis
            redisTemplate.opsForValue().set(key, list);
            return Result.success(list);
        }
    }
}