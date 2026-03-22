package com.zqw.qwpicturebackend.model.dto.picture;

import com.zqw.qwpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 搜索词（同时搜名称、简介等）
     */
    private String searchText;

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

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 图片空间id
     */
    private Long spaceId;

    /**
     * 新增根据时间段进行搜索
     */
    private Date startEditTime;

    /**
     * 新增根据时间段进行搜索
     */
    private Date endEditTime;

    /**
     * 是否查询公共图库中 空间id为 null的图片
     * todo 为什么不能只用 spaceId作为查询条件，如果为null不久查询公共图片了吗
     *  因为在拼接查询参数时我们进行 null判断，只有不为null的才被拼接为查询条件
     */
    private boolean nullSpaceId;

    private static final long serialVersionUID = 1L;
}

