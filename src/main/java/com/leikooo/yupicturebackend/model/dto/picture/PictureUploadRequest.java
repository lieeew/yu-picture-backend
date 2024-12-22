package com.leikooo.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author leikooo
 */
@Data
public class PictureUploadRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1989537749944365432L;

    /**
     * 图片 id（用于修改）
     */
    private Long id;


    /**
     * 图片名称
     */
    private String picName;


    private String fileUrl;

}
