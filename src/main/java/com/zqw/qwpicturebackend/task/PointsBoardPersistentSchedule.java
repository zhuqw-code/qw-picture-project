package com.zqw.qwpicturebackend.task;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zqw.qwpicturebackend.model.entity.PictureLikeRank;
import com.zqw.qwpicturebackend.service.PictureLikeRankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.management.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Slf4j
public class PointsBoardPersistentSchedule {

    @Resource
    private PictureLikeRankService pictureLikeRankService;

    /**
     * 创建定时任务
     */
    @Scheduled(cron = "0 */1 * * * *")
    public void createTask() {
        // todo 后续修改为根据 时间删除过期的收藏数据
        // 每个5分钟执行一次，非排行数据
        // 1. 首先获取所有根据like 排序的图片id，删除掉大于88的记录
        QueryWrapper<PictureLikeRank> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("likeCount");
        queryWrapper.select("id");   // 只返回id，其他字段为空
        List<PictureLikeRank> idList = pictureLikeRankService.list(queryWrapper);
        int[] ids = IntStream.range(20, idList.size()).map(i -> idList.get(i).getId().intValue()).toArray();
        // 批量删除
        pictureLikeRankService.removeBatchByIds(Arrays.stream(ids).boxed().collect(Collectors.toList()));

        log.info("定时任务批量删除成功");
    }
}
