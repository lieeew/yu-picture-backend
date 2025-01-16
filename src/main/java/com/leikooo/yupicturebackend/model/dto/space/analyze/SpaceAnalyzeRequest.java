package com.leikooo.yupicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1488267001433077424L;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;

}
