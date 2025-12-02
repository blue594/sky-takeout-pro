package com.sky.mapper;

import com.sky.entity.GuessYouLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface GuessYouLikeMapper {

    /**
     * 批量插入猜你喜欢数据
     */
    void insertBatch(List<GuessYouLike> list);

    /**
     * 根据用户id查询猜你喜欢数据
     */
    @Select("select * from guess_you_like where user_id = #{userId} order by score")
    List<GuessYouLike> listByUserId(Long userId);

    /**
     * 根据用户id删除旧数据（用于更新推荐时清空历史）
     */
    @Delete("delete from guess_you_like where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /**
     * 检查用户是否已有猜你喜欢数据
     */
    @Select("select count(*) from guess_you_like where user_id = #{userId}")
    Integer countByUserId(Long userId);
}