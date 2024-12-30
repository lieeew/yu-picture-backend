package com.leikooo.yupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间请求
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Data
public class SpaceDeleteRequest implements Serializable {
    /**
     * spaceId
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}