package com.zqw.qwpicturebackend.service.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

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
        ArrayList<PictureRankVO> rankList = new ArrayList<>();
        // 直接从redis中获取排名信息
        Set<String> rangeSet = stringRedisTemplate.opsForZSet().reverseRange(RedisConstant.RANK_KEY + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd")), 0, -1);
        Map<Long, Picture> collect = rangeSet.stream()
                .map(key -> pictureService.getById(key))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Picture::getId,
                        Function.identity(),
                        (key1, key2) -> key1
                ));

        for (String key : rangeSet) {
            LocalDate now = LocalDate.now();
            String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM"));
            // 获取value
            int score = Objects.requireNonNull(stringRedisTemplate.opsForZSet().score(RedisConstant.RANK_KEY + format, key)).intValue();
            int rank = Objects.requireNonNull(stringRedisTemplate.opsForZSet().reverseRank(RedisConstant.RANK_KEY + format, key)).intValue() + 1;
            String pictureName = collect.getOrDefault(Long.valueOf(key), Picture.builder().name("未定义").build()).getName();
            PictureRankVO pictureRankVO = new PictureRankVO();
            pictureRankVO.setRank(rank);
            pictureRankVO.setPictureId(Long.valueOf(key));
            pictureRankVO.setPictureName(pictureName);
            pictureRankVO.setLikeCount(score);
            rankList.add(pictureRankVO);
        }
        return rankList;
    }
}




