package com.sky.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 猜你喜欢关联表（用户与推荐菜品/套餐的映射）
 */
@Data
public class GuessYouLike implements Serializable {
    private Long id;
    private Long userId;
    private Long dishId;       // 菜品ID（与setmealId二选一）
    private Long setmealId;    // 套餐ID（与dishId二选一）
    private Integer score;     // 推荐优先级分数
    private LocalDateTime createTime;
}