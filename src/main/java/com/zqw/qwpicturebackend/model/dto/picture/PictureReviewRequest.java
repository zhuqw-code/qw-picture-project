package com.zqw.qwpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员请求修改图片状态的请求类
 */
@Data
public class PictureReviewRequest implements Serializable {
    /**
     * 待修改的图片id
     */
    private Long id;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 ID
     */
    private Long reviewerId;

    private static final long serialVersionUID = -503143494037195156L;
}
