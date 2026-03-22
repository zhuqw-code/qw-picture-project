package com.zqw.qwpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.exception.ThrowUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审核状态枚举类
 */
@Getter
public enum PictureReviewStatusEnum {
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);
    final String text;
    final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    private static final Map<Integer, PictureReviewStatusEnum> PICTURE_REVIEW_STATUS_ENUM_MAP =
            Arrays.stream(PictureReviewStatusEnum.values()).collect(Collectors.toMap(PictureReviewStatusEnum::getValue, e -> e));

    /**
     * 编写静态方法根据value 获取到枚举信息
     *
     * @param value 传入的值
     * @return 返回枚举信息
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        PictureReviewStatusEnum pictureReviewStatusEnum = value == null ? null : PICTURE_REVIEW_STATUS_ENUM_MAP.getOrDefault(value, null);
        ThrowUtils.throwif(ObjUtil.isNotNull(pictureReviewStatusEnum), ErrorCode.PARAMS_ERROR, "请求状态不存在~");
        return pictureReviewStatusEnum;
    }
}
