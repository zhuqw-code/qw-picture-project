package com.zqw.qwpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zqw.qwpicturebackend.constant.RedisConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.mapper.PictureLikeRankMapper;
import com.zqw.qwpicturebackend.model.entity.Picture;
import com.zqw.qwpicturebackend.model.entity.PictureLikeRank;
import com.zqw.qwpicturebackend.model.vo.PictureRankVO;
import com.zqw.qwpicturebackend.model.vo.PictureVO;
import com.zqw.qwpicturebackend.service.PictureLikeRankService;
import com.zqw.qwpicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.support.collections.DefaultRedisZSet;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
* @author zhuqw
* @description 针对表【picture_like_rank(图片点赞记录表)】的数据库操作Service实现
* @createDate 2026-04-10 11:28:49
*/
@Slf4j
@Service
public class PictureLikeRankServiceImpl extends ServiceImpl<PictureLikeRankMapper, PictureLikeRank>
    implements PictureLikeRankService{

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PictureService pictureService;

    @Override
    public void increase(Long pictureId) {
        // 使用lambda
        PictureLikeRank pictureLikeRank = this.getById(pictureId);
        if (ObjectUtils.isEmpty(pictureLikeRank)) {
            pictureLikeRank = new PictureLikeRank();
            pictureLikeRank.setPictureId(pictureId);
            pictureLikeRank.setLikeCount(0L);
        }
        Long likeCount = pictureLikeRank.getLikeCount();
        // 进行更新或修改
        pictureLikeRank.setLikeCount(likeCount + 1);
        boolean isOk = false;
        try {
            saveOrUpdate(pictureLikeRank);    // 更新失败我们直接抛异常
        } catch (Exception e) {
            // 修改失败
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作失败");
        }

        // 3. 更新缓存
        // 获取当前日期
        LocalDate now = LocalDate.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM"));
        // 有对应的value就increase，没有就add
        stringRedisTemplate.opsForZSet().incrementScore(RedisConstant.RANK_KEY + date, String.valueOf(pictureId), pictureLikeRank.getLikeCount());
    }

    /**
     * 为什么使用 redis还要使用 数据库进行查询，因为要获取最新数据，缓存的数据可能被其他用户修改，并且这里缓存充当排序
     */
    @Override
    public List<PictureRankVO> listRank() {
        // 直接从redis中获取排名信息
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM");
        String format = RedisConstant.RANK_KEY + LocalDate.now().format(formatter);
        // Set<String> rangeSet = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.RANK_KEY + format, 0, -1);
        // 一次查询出成员 id + score
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet() // 直接携带拿到 score +  rank
                .reverseRangeWithScores(format, 0, -1);

        List<Long> keyIds = typedTuples.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .collect(Collectors.toList());

        if (ObjectUtils.isEmpty(keyIds)) {
            return Collections.emptyList();
        }

        // 查询出所有图片
        List<Picture> pictures = pictureService.listByIds(keyIds);

        // 将图片进行映射
        Map<Long, String> nameMaps = pictures.stream().collect(Collectors.toMap(Picture::getId, Picture::getName));

        ArrayList<PictureRankVO> rankList = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples){
            // 获取 pictureId
            Long pictureId = Long.valueOf(Objects.requireNonNull(tuple.getValue()));
            int score = Objects.requireNonNull(tuple.getScore()).intValue();
            String pictureName = nameMaps.get(pictureId);
            PictureRankVO pictureRankVO = new PictureRankVO();
            pictureRankVO.setRank(rank++);
            pictureRankVO.setPictureId(pictureId);
            pictureRankVO.setPictureName(pictureName != null ? pictureName : "未设置");
            pictureRankVO.setLikeCount(score);
            rankList.add(pictureRankVO);
        }
        return rankList;
    }

    /**
     * 缓存预热
     */
    @PostConstruct
    public void preheat() {
        // 获取数据库中前100数据
        QueryWrapper<PictureLikeRank> queryWrapper = new QueryWrapper();
        queryWrapper.orderByDesc("likeCount");
        queryWrapper.last("limit 88");
        List<PictureLikeRank> list = this.list(queryWrapper);
        // 存入缓存中 【key, score】
        // 性能不好
        // list.stream().forEach(rank -> {
        //     stringRedisTemplate.opsForZSet().add(RedisConstant.RANK_KEY + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM")), String.valueOf(rank.getPictureId()), rank.getLikeCount());
        // });
        // 性能好
        Set<ZSetOperations.TypedTuple<String>> collect = list.stream()
                .map(rank -> new DefaultTypedTuple<>(String.valueOf(rank.getPictureId()), rank.getLikeCount().doubleValue())).collect(Collectors.toSet());

        String format = RedisConstant.RANK_KEY + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM"));
        stringRedisTemplate.opsForZSet().add(format, collect);
        log.info("缓存预热成功");
    }
}




