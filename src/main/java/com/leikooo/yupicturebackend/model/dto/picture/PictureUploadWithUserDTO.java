package com.leikooo.yupicturebackend.model.dto.picture;

import com.leikooo.yupicturebackend.model.entity.User;
import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 *
 * @author 程序员鱼皮 <a href="https://www.codefather.cn">编程导航原创项目</a>
 */
@Data
public class PictureUploadWithUserDTO implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 登录的用户
     */
    private User user;

    private static final long serialVersionUID = 1L;
}