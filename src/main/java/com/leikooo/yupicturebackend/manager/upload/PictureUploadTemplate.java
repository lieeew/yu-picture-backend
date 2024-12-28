package com.leikooo.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.manager.CosManager;
import com.leikooo.yupicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author liang
 * @description 图片上传抽象类 模板方法
 * @date 2024-12-16 11:56:07
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosManager cosManager;

    /**
     * 限制文件大小为 2MB
     */
    protected final long TWO_MB = 10 * 1024 * 1024L;

    /**
     * 上传图片接口
     *
     * @param obj url 或者  multipartFile
     * @return 封装的 VO
     */
    public UploadPictureResult uploadPicture(Object obj, String uploadPathPrefix) {
        checkParamSource(obj);
        String templatePath = System.getProperty("user.dir") + File.separator + getOriginFilename(obj);
        File tempFile = new File(templatePath);
        File file = processFile(obj, tempFile);
        // 上传文件
        return uploadPicture(file, uploadPathPrefix);
    }

    /**
     * 处理文件
     *
     * @param object 内容来源
     */
    protected abstract File processFile(Object object, File file);

    /**
     * 校验参数
     */
    protected abstract void checkParamSource(Object object);

    /**
     * 获取 FileName
     */
    protected abstract String getOriginFilename(Object object);

    public UploadPictureResult uploadPicture(File file, String uploadPathPrefix) {
        String imagePath = generateImageUploadPath(file, uploadPathPrefix);
        try {
            return analyzeCosReturn(new AnalyzeCosParams(
                    cosManager.putPictureObject(imagePath, file),
                    FileUtil.mainName(file),
                    imagePath
            ));
        } catch (Exception e) {
            log.error("Error uploading picture: {}", ExceptionUtils.getRootCauseMessage(e), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            try {
                FileUtil.del(file);
            } catch (IORuntimeException e) {
                log.error("Error deleting temp file: {}", file.getAbsolutePath(), e);
            }
        }
    }

    private String generateImageUploadPath(File file, String uploadPathPrefix) {
        String originalFilename = FileUtil.getName(file);
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), RandomUtil.randomString(16),
                originalFilename);
        // 最后结果 public/1867564572229492994/2024-12-21_REArPZjceu7DkRp3.Konachan.jpg
        return String.format("%s/%s", uploadPathPrefix, uploadFilename);
    }

    /**
     * 获取文件后缀，默认转成小写进行判断
     *
     * @param fileName 文件名
     * @return 文件后缀
     */
    protected String extractFileSuffix(String fileName) {
        return Optional.of(fileName)
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .map(String::toLowerCase)
                .orElse("");
    }

    private UploadPictureResult analyzeCosReturn(AnalyzeCosParams params) {
        PutObjectResult putObjectResult = params.getPutObjectResult();
        CIUploadResult ciUploadResult = putObjectResult.getCiUploadResult();
        ImageInfo imageInfo = ciUploadResult.getOriginalInfo().getImageInfo();
        List<CIObject> objectList = ciUploadResult.getProcessResults().getObjectList();
        if (CollUtil.isNotEmpty(objectList)) {
            CIObject compressedCiObject = objectList.get(0);
            // 缩略图默认等于压缩图
            CIObject thumbnailCiObject = compressedCiObject;
            // 有生成缩略图，才得到缩略图
            if (objectList.size() > 1) {
                thumbnailCiObject = objectList.get(1);
            }
            // 封装压缩图返回结果
            return buildResult(params.getImageName(), compressedCiObject, thumbnailCiObject);
        }
        return buildResult(params, imageInfo);
    }

    private UploadPictureResult buildResult(String fileName, CIObject ciObject, CIObject thumbnailObject) {
        return UploadPictureResult.builder()
                .picFormat(ciObject.getFormat())
                .picHeight(ciObject.getHeight())
                .picWidth(ciObject.getWidth())
                .picSize((long) ciObject.getQuality())
                .picScale(NumberUtil.round(ciObject.getHeight() * 1.0 / ciObject.getWidth(), 2).doubleValue())
                .picName(fileName)
                .url(String.format("%s/%s", cosManager.getBaseUrl(), ciObject.getKey()))
                .thumbnailUrl(String.format("%s/%s", cosManager.getBaseUrl(), thumbnailObject.getKey()))
                .build();
    }

    private UploadPictureResult buildResult(AnalyzeCosParams params, ImageInfo imageInfo) {
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




