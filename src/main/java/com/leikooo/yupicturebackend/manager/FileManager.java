package com.leikooo.yupicturebackend.manager;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Component
@Slf4j
@Deprecated
public class FileManager {
    private static final long ONE_MB = 1024 * 1024L;

    private static final long MAX_FILE_SIZE = 2 * ONE_MB;

    private final CosManager cosManager;

    public FileManager(CosManager cosManager) {
        this.cosManager = cosManager;
    }

    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        return uploadPictureInternal(
                multipartFile,
                () -> File.createTempFile(generateTempFileName(multipartFile.getOriginalFilename()), null),
                uploadPathPrefix
        );
    }

    public UploadPictureResult uploadPicture(File file, String uploadPathPrefix) {
        return uploadPictureInternal(file, () -> file, uploadPathPrefix);
    }

    private <T> UploadPictureResult uploadPictureInternal(T file, FileProvider<T> fileProvider, String uploadPathPrefix) {
        validateFile(file);
        String imagePath = generateImageUploadPath(file, uploadPathPrefix);
        File tempFile = null;

        try {
            tempFile = fileProvider.getFile();
            if (file instanceof MultipartFile multipartFile) {
                multipartFile.transferTo(tempFile);
            }

            return analyzeCosReturn(new AnalyzeCosParams(
                    cosManager.putPictureObject(imagePath, tempFile),
                    extractFileName(file),
                    imagePath
            ));
        } catch (Exception e) {
            log.error("Error uploading picture: {}", ExceptionUtils.getRootCauseMessage(e), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private <T> void validateFile(T file) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        long fileSize = (file instanceof MultipartFile multipartFile) ? multipartFile.getSize() : ((File) file).length();
        if (fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        }

        String fileSuffix = extractFileSuffix(file);
//        if (!AllowFileTypeConstant.isValidFileTypeForUpdate(fileSuffix)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
//        }
    }

    private <T> String generateImageUploadPath(T file, String uploadPathPrefix) {
        String fileName = extractFileName(file);
        String randomFileName = String.format("%s_%s.%s", LocalDate.now(), RandomUtil.randomString(16), fileName);
        return String.format("%s/%s", uploadPathPrefix, randomFileName);
    }

    private <T> String extractFileName(T file) {
        return file instanceof MultipartFile multipartFile
                ? multipartFile.getOriginalFilename()
                : ((File) file).getName();
    }

    private <T> String extractFileSuffix(T file) {
        String fileName = extractFileName(file);
        return Optional.ofNullable(fileName)
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .orElse("");
    }

    private String generateTempFileName(String originalFilename) {
        return String.format("%s_%s", LocalDate.now(), RandomUtil.randomString(8)) +
                (originalFilename != null ? originalFilename.hashCode() : "");
    }

    private void deleteTempFile(@Nullable File file) {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            if (!file.delete()) {
                log.warn("Failed to delete temp file: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error deleting temp file: {}", file.getAbsolutePath(), e);
        }
    }

    private UploadPictureResult analyzeCosReturn(AnalyzeCosParams params) {
        ImageInfo imageInfo = params.getPutObjectResult().getCiUploadResult().getOriginalInfo().getImageInfo();
        return UploadPictureResult.builder()
                .picFormat(imageInfo.getFormat())
                .picHeight(imageInfo.getHeight())
                .picWidth(imageInfo.getWidth())
                .picSize((long) imageInfo.getQuality())
                .picScale(NumberUtil.round(imageInfo.getHeight() * 1.0 / imageInfo.getWidth(), 2).doubleValue())
                .picName(params.getImageName())
                .url(String.format("%s/%s", cosManager.getBaseUrl(), params.getImagePath()))
                .build();
    }

    @FunctionalInterface
    private interface FileProvider<T> {
        File getFile() throws IOException;
    }

    /**
     * 不用成员变量因为多线程时会出问题
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class AnalyzeCosParams {
        private PutObjectResult putObjectResult;
        private String imageName;
        private String imagePath;
    }
}

