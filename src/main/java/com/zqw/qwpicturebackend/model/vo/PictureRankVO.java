package com.zqw.qwpicturebackend.model.vo;

import lombok.Data;


/**
 * 图片点赞排行榜 VO (仅含前端展示关键字段)
 */
@Data
public class PictureRankVO {

    /**
     * 排名 (1 = 第1名，前端可直接展示)
     */
    private Integer rank;

    /**
     * 图片 ID (用于点击跳转详情等交互)
     */
    private Long pictureId;

    /**
     * 图片名称 (前端核心展示字段)
     */
    private String pictureName;

    /**
     * 点赞数 (前端核心展示字段)
     */
    private Integer likeCount;
}
