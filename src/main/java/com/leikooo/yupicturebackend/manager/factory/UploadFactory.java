package com.leikooo.yupicturebackend.manager.factory;

import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.manager.upload.FilePictureUpload;
import com.leikooo.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.leikooo.yupicturebackend.manager.upload.UrlPictureUpload;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/21
 * @description
 */
@Component
public class UploadFactory {
    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    private final HashMap<String, PictureUploadTemplate> UPLOAD_FACTORY = new HashMap<>(2);

    @PostConstruct
    public void init() {
        UPLOAD_FACTORY.put("file", filePictureUpload);
        UPLOAD_FACTORY.put("url", urlPictureUpload);
    }

    public PictureUploadTemplate getUploadFactory(String type) {
        PictureUploadTemplate uploadFactory = UPLOAD_FACTORY.get(type);
        ThrowUtils.throwIf(uploadFactory == null, ErrorCode.PARAMS_ERROR, "上传类型错误");
        return uploadFactory;
    }
}
