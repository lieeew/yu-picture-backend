package com.leikooo.yupicturebackend.manager.factory;

import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.manager.upload.PictureUploadTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/21
 * @description
 */
@Component
public class UploadFactory {
    @Resource
    private List<PictureUploadTemplate> uploadPictureTemplates;

    private final HashMap<String, PictureUploadTemplate> uploadFactory = new HashMap<>(10);

    @PostConstruct
    private void init() {
        // 初始化方便后面调用
        uploadPictureTemplates.forEach(uploadTemplate -> {
            Component annotation = uploadTemplate.getClass().getAnnotation(Component.class);
            if (annotation != null && StringUtils.isNotBlank(annotation.value())) {
                uploadFactory.put(annotation.value(), uploadTemplate);
            }
        });
    }

    public PictureUploadTemplate getUploadFactory(String type) {
        PictureUploadTemplate pictureUploadTemplate = uploadFactory.getOrDefault(type, null);
        ThrowUtils.throwIf(pictureUploadTemplate == null, ErrorCode.NOT_FOUND_ERROR, "上传类型不存在");
        return pictureUploadTemplate;
    }
}
