package com.leikooo.yupicturebackend.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
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

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Map<String, UserRoleEnum> userRoleEnumMap = Arrays.stream(UserRoleEnum.values()).
                collect(Collectors.toMap(UserRoleEnum::getValue, userRoleEnum -> userRoleEnum));
        return userRoleEnumMap.getOrDefault(value, null);
    }
}
