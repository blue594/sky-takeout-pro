package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> Page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        PageResult pageResult =dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result delete(@RequestParam List<Long> ids){
        //查询出选择的菜品属于的分类（由于redis中存入的单条数据是分类数据，需要把所属分类的redis数据删除重建）
        List<Integer> categoryIds = dishService.getCategoryIdByDishIds(ids);
        dishService.delete(ids);
        for (Integer categoryId : categoryIds) {
            String key = "dish_" + categoryId;
            redisTemplate.delete(key);
        }

        return Result.success();
    }

    /**
     * 根据id获取菜品信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id获取菜品信息")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id获取菜品信息:{}",id);
        DishVO dishVO = dishService.getByIdWithFlavors(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品信息
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品信息")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品信息:{}",dishDTO);
        dishService.update(dishDTO);
        // 清理该菜品所属分类的缓存
        String categoryKey = "dish_" + dishDTO.getCategoryId();
        redisTemplate.delete(categoryKey);
        // 额外清理所有用户的"猜你喜欢"缓存（因为月销量变化会影响推荐结果）
        Set<String> guessKeys = redisTemplate.keys("dish_guess_you_like_*");
        if(guessKeys != null && !guessKeys.isEmpty()){
            redisTemplate.delete(guessKeys);
        }
        return Result.success();
    }

    /**
     * 修改菜品状态
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品状态")
    public Result startOrStop(@PathVariable Integer status,Long id){
        Dish dish = dishService.getById(id);
        Long categoryId = dish.getCategoryId();
        dishService.startOrStop(status,id);
        redisTemplate.delete("dish_"+categoryId);
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> dishList = dishService.list(categoryId);
        return Result.success(dishList);
    }

    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
