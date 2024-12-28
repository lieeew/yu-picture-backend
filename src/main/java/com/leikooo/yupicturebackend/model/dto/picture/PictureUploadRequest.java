package com.leikooo.yupicturebackend.model.dto.picture;

import com.leikooo.yupicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

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

    /**
     * 图片 url
     */
    private String fileUrl;

    /**
     * 空间 id
     */
    private Long spaceId;

    public PictureUploadWithUserDTO toPictureUploadWithUserDTO(User loginUser) {
        PictureUploadWithUserDTO pictureUploadWithUserDTO = new PictureUploadWithUserDTO();
        BeanUtils.copyProperties(this, pictureUploadWithUserDTO);
        pictureUploadWithUserDTO.setUser(loginUser);
        return pictureUploadWithUserDTO;
    }
}
