package com.leikooo.yupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.leikooo.yupicturebackend.config.CosClientConfig;
import com.leikooo.yupicturebackend.utils.ConvertToPinyinUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Component
@AllArgsConstructor
public class CosManager {

    private CosClientConfig cosClientConfig;

    private COSClient cosClient;

    private final List<String> ALLOW_FILE_TYPE = Arrays.asList("png", "jpg", "jpeg");

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest =
                new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }


    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        // 缩略图处理
        // 缩略图处理，仅对 > 20 KB 的图片生成缩略图
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));
            rules.add(thumbnailRule);
        }
        // 图片格式转换 如果不是 png/jpg/jpeg 进行转化成 jpg 格式，方便后面百度图搜图接口的使用
        if (!ALLOW_FILE_TYPE.contains(FileUtil.getSuffix(key))) {
            PicOperations.Rule transferRule = new PicOperations.Rule();
            transferRule.setBucket(cosClientConfig.getBucket());
            transferRule.setRule("imageMogr2/format/png");
            String transferKey = FileUtil.mainName(key) + "_transfer" + ".png";
            transferRule.setFileId(transferKey);
            rules.add(transferRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    public String getBaseUrl() {
        return cosClientConfig.getHost();
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 删除对象
     *
     * @param keys List 集合
     */
    public void deleteObjects(List<String> keys) {
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(cosClientConfig.getBucket());
        List<DeleteObjectsRequest.KeyVersion> keyVersions = new ArrayList<>();
        deleteObjectsRequest.setKeys(keyVersions);
        keys.forEach(key -> keyVersions.add(new DeleteObjectsRequest.KeyVersion(key)));
        deleteObjectsRequest.setKeys(keyVersions);
        cosClient.deleteObjects(deleteObjectsRequest);
    }
}
