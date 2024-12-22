package com.leikooo.yupicturebackend.model.enums;

import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import lombok.Getter;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/22
 * @description
 */
@Getter
public enum FileUploadEnum {
    FILE("file"),

    URL("url");

    private final String type;

    FileUploadEnum(String type) {
        this.type = type;
    }

    public static String getType(@NonNull String type) {
        for (FileUploadEnum fileUploadEnum : FileUploadEnum.values()) {
            if (fileUploadEnum.getType().equals(type)) {
                return fileUploadEnum.getType();
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的上传类型");
    }
}
