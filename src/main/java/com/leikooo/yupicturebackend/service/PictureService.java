package com.leikooo.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.leikooo.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.leikooo.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.leikooo.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author liang
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2024-12-16 11:56:07
 */
public interface PictureService {

    /**
     * 上传图片
     *
     * @param object        文件/url
     * @param pictureUploadRequest 请求类
     * @param loginUser            当前登录的 user
     * @return picture 的封装对象
     */
    PictureVO uploadPicture(Object object,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest pictureUploadByBatchRequest
     * @param loginUser 登录的用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );


    /**
     * 获取查询的 queryWrapper
     *
     * @param pictureQueryRequest 图片请求类
     * @return 可用来查询的 queryWrapper
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片的 VO 对象
     *
     * @param picture picture 对象
     * @param request request 请求
     * @return 对应图片的 VO
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片 VO 对象
     *
     * @param picturePage page 对象
     * @param request     request 请求
     * @return 分页的 VO
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验参数
     *
     * @param picture 需要校验的 picture 对象
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            当前登录的 user
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数，方便其他方法使用
     *
     * @param picture   picture
     * @param loginUser 登录的用户
     */
    void fillReviewParams(Picture picture, User loginUser);
}
