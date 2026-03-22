package com.zqw.qwpicturebackend.model.enums;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一管理用户角色
 */
@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    /**
     * 中文名称  （用户/管理员）
     */
    private final String text;

    /**
     * user/admin
     */
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 通过 value 值获取到枚举信息 【但是没有想到到底有什么用，反正是用来优化获取的】
     *
     * @param value 传递的 value
     * @return 有对应 value 的枚举值就返回，没有就返回null
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Map<String, UserRoleEnum> userRoleMap = Arrays.stream(UserRoleEnum.values())
                .collect(Collectors.toMap(UserRoleEnum::getValue, userRoleEnum -> userRoleEnum));
        return userRoleMap.getOrDefault(value, null);
    }
}
