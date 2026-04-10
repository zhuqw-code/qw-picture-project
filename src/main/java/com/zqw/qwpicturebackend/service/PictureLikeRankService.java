package com.zqw.qwpicturebackend.service;

import com.zqw.qwpicturebackend.model.entity.PictureLikeRank;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zqw.qwpicturebackend.model.vo.PictureRankVO;

import java.util.List;

/**
* @author zhuqw
* @description 针对表【picture_like_rank(图片点赞记录表)】的数据库操作Service
* @createDate 2026-04-10 11:28:49
*/
public interface PictureLikeRankService extends IService<PictureLikeRank> {

    /**
     * 不限制人的点赞次数
     * @param pictureId
     */
    void increase(Long pictureId);

    List<PictureRankVO> listRank();
}
