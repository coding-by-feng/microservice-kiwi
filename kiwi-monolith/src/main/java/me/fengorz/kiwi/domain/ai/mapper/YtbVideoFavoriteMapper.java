/*
 * Copyright [2019~2025] [codingByFeng]
 */
package me.fengorz.kiwi.domain.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.fengorz.kiwi.domain.ai.entity.YtbVideoFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface YtbVideoFavoriteMapper extends BaseMapper<YtbVideoFavorite> {

    /**
     * Check if a video is favorited by user using a single JOIN query.
     * This is more efficient than querying video first, then checking favorite.
     */
    @Select("SELECT COUNT(1) FROM ytb_channel_video v " +
            "INNER JOIN ytb_video_favorite f ON v.id = f.video_id " +
            "WHERE v.video_link = #{videoUrl} " +
            "AND f.user_id = #{userId} " +
            "AND f.if_valid = 1")
    int countByVideoUrlAndUserId(@Param("videoUrl") String videoUrl, @Param("userId") Long userId);
}
