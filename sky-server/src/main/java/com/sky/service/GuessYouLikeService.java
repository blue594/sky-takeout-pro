package com.sky.service;

import java.util.List;

public interface GuessYouLikeService {

    /**
     * 检查并初始化用户的猜你喜欢数据
     * @param userId 用户id
     */
    void initOrUpdateGuessYouLike(Long userId);

    /**
     * 查询用户的猜你喜欢数据（包含菜品和套餐）
     *
     * @param userId 用户id
     * @return 键为"dishes"和"setmeals"的Map
     */
    List<Object> getGuessYouLikeData(Long userId);
}