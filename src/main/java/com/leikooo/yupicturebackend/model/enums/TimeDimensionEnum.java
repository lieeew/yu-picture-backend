package com.leikooo.yupicturebackend.model.enums;

import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/22
 * @description
 */
@Getter
public enum TimeDimensionEnum {
    DAY("day"),
    WEEK("week"),
    MONTH("month");

    private final String value;

    TimeDimensionEnum(String value) {
        this.value = value;
    }

    public static TimeDimensionEnum getEnumByValue(@NonNull String value) {
        for (TimeDimensionEnum timeDimensionEnum : TimeDimensionEnum.values()) {
            if (timeDimensionEnum.value.equals(value)) {
                return timeDimensionEnum;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度不存在");
    }
}
