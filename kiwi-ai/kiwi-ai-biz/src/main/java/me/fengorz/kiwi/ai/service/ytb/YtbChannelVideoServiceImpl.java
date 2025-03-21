package me.fengorz.kiwi.ai.service.ytb;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.fengorz.kiwi.ai.api.entity.YtbChannelVideoDO;
import me.fengorz.kiwi.ai.api.vo.ytb.YtbChannelVideoVO;
import me.fengorz.kiwi.ai.service.ytb.mapper.YtbChannelVideoMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YtbChannelVideoServiceImpl extends ServiceImpl<YtbChannelVideoMapper, YtbChannelVideoDO> implements YtbChannelVideoService {

    @Override
    public IPage<YtbChannelVideoVO> getVideosByChannelId(Page<YtbChannelVideoDO> page, Long channelId) {
        IPage<YtbChannelVideoDO> videoPage = this.page(page, new LambdaQueryWrapper<YtbChannelVideoDO>()
                .eq(YtbChannelVideoDO::getChannelId, channelId)
                .eq(YtbChannelVideoDO::getIfValid, true)
                .orderByDesc(YtbChannelVideoDO::getCreateTime));
        
        // Convert DO to VO
        List<YtbChannelVideoVO> videoVOList = videoPage.getRecords().stream()
                .map(video -> {
                    YtbChannelVideoVO vo = new YtbChannelVideoVO();
                    BeanUtils.copyProperties(video, vo);
                    // Add status information (simplified)
                    vo.setStatus("Ready"); // This would be derived from subtitles processing status
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