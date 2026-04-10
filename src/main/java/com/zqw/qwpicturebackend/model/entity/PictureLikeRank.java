package com.zqw.qwpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 图片点赞记录表
 * @TableName picture_like_rank
 */
@TableName(value ="picture_like_rank")
@Data
public class PictureLikeRank {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片唯一标识ID
     */
    private Long pictureId;


    /**
     * 点赞次数 (如果是单用户单次记录，此字段可忽略)
     */
    private Long likeCount;
}