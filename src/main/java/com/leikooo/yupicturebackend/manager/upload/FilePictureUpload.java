package com.leikooo.yupicturebackend.manager.upload;

import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/21
 * @description
 */
@Component("file")
public class FilePictureUpload extends PictureUploadTemplate {

    private final static List<String> ALLOW_FILE_TYPE = Arrays.asList("jpeg", "jpg", "png", "webp");

    @Override
    protected File processFile(Object object, File file) {
        MultipartFile multipartFile = (MultipartFile) object;
        try {
             multipartFile.transferTo(file);
             return file;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
        }
    }

    @Override
    protected void checkParamSource(Object object) {
        ThrowUtils.throwIf(object == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        MultipartFile multipartFile = (MultipartFile) object;
        String filename = multipartFile.getOriginalFilename();
        String fileSuffix = extractFileSuffix(filename);
        if (!ALLOW_FILE_TYPE.contains(fileSuffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
        long fileSize = multipartFile.getSize();
        if (fileSize > TWO_MB) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        }
    }

    @Override
    protected String getOriginFilename(Object object) {
        MultipartFile multipartFile = (MultipartFile) object;
        return multipartFile.getOriginalFilename();
    }
}
