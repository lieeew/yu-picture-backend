package com.leikooo.yupicturebackend.model.enums;

import com.google.common.collect.ImmutableMap;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/12
 * @description
 */
@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    private final static Map<String, UserRoleEnum> USER_ROLE_ENUM_MAP = ImmutableMap.copyOf(Arrays.stream(UserRoleEnum.values()).
            collect(Collectors.toMap(UserRoleEnum::getValue, userRoleEnum -> userRoleEnum)));

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        UserRoleEnum userRoleEnum = value == null ? null : USER_ROLE_ENUM_MAP.getOrDefault(value, null);
        ThrowUtils.throwIf(Objects.isNull(userRoleEnum), ErrorCode.PARAMS_ERROR, "角色不存在");
        return userRoleEnum;
    }
}
