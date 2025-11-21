package me.fengorz.kiwi.ai.service.ytb;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.YtbChannelVideoDO;
import me.fengorz.kiwi.ai.api.entity.YtbVideoFavoriteDO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVideoVO;
import me.fengorz.kiwi.ai.service.ytb.mapper.YtbChannelVideoMapper;
import me.fengorz.kiwi.ai.service.ytb.mapper.YtbVideoFavoriteMapper;
import me.fengorz.kiwi.common.sdk.web.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YtbChannelVideoServiceImpl extends ServiceImpl<YtbChannelVideoMapper, YtbChannelVideoDO>
                implements YtbChannelVideoService {

        private final YtbVideoFavoriteMapper videoFavoriteMapper;

        @Override
        public IPage<YtbChannelVideoVO> getVideosByChannelId(Page<YtbChannelVideoDO> page, Long channelId) {
                IPage<YtbChannelVideoDO> videoPage = this.page(page, new LambdaQueryWrapper<YtbChannelVideoDO>()
                                .eq(YtbChannelVideoDO::getChannelId, channelId)
                                .eq(YtbChannelVideoDO::getIfValid, true)
                                .orderByDesc(YtbChannelVideoDO::getPublishedAt)
                                .orderByDesc(YtbChannelVideoDO::getCreateTime));

                // Batch favorite data
                List<Long> videoIds = videoPage.getRecords().stream().map(YtbChannelVideoDO::getId)
                                .collect(Collectors.toList());
                Integer userId = SecurityUtils.getCurrentUserId();
                Set<Long> userFavSet = new HashSet<>();
                Map<Long, Long> favCountMap = new HashMap<>();
                if (!videoIds.isEmpty()) {
                        List<YtbVideoFavoriteDO> userFavs = videoFavoriteMapper
                                        .selectList(new LambdaQueryWrapper<YtbVideoFavoriteDO>()
                                                        .eq(YtbVideoFavoriteDO::getUserId, userId)
                                                        .in(YtbVideoFavoriteDO::getVideoId, videoIds)
                                                        .eq(YtbVideoFavoriteDO::getIfValid, true));
                        userFavSet.addAll(userFavs.stream().map(YtbVideoFavoriteDO::getVideoId)
                                        .collect(Collectors.toSet()));

                        List<YtbVideoFavoriteDO> allFavs = videoFavoriteMapper
                                        .selectList(new LambdaQueryWrapper<YtbVideoFavoriteDO>()
                                                        .in(YtbVideoFavoriteDO::getVideoId, videoIds)
                                                        .eq(YtbVideoFavoriteDO::getIfValid, true));
                        favCountMap.putAll(allFavs.stream().collect(
                                        Collectors.groupingBy(YtbVideoFavoriteDO::getVideoId, Collectors.counting())));
                }

                // Convert DO to VO
                List<YtbChannelVideoVO> videoVOList = videoPage.getRecords().stream()
                                .map(video -> {
                                        YtbChannelVideoVO vo = new YtbChannelVideoVO();
                                        BeanUtils.copyProperties(video, vo);
                                        vo.setFavorited(userFavSet.contains(video.getId()));
                                        vo.setFavoriteCount(favCountMap.getOrDefault(video.getId(), 0L));
                                        return vo;
                                })
                                .collect(Collectors.toList());

                // Create a new page with VO list
                Page<YtbChannelVideoVO> voPage = new Page<>();
                voPage.setCurrent(videoPage.getCurrent());
                voPage.setSize(videoPage.getSize());
                voPage.setTotal(videoPage.getTotal());
                voPage.setRecords(videoVOList);

                return voPage;
        }
}