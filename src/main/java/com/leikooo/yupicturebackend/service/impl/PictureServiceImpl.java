package com.leikooo.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.dao.UserDAO;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.manager.factory.UploadFactory;
import com.leikooo.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.leikooo.yupicturebackend.model.dto.file.UploadPictureResult;
import com.leikooo.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.leikooo.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.leikooo.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.leikooo.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.enums.FileUploadEnum;
import com.leikooo.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.leikooo.yupicturebackend.model.vo.PictureVO;
import com.leikooo.yupicturebackend.model.vo.UserVO;
import com.leikooo.yupicturebackend.service.PictureService;
import com.leikooo.yupicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liang
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @date 2024-12-16 11:56:07
 */
@Slf4j
@Service
@AllArgsConstructor
public class PictureServiceImpl implements PictureService {

    private PictureDAO pictureDAO;

    private UserDAO userDAO;

    private UserService userService;

    private UploadFactory uploadFactory;

    /**
     * 上传图片接口
     *
     * @param object               文件/url
     * @param pictureUploadRequest 请求类
     * @param loginUser            当前登录的 user
     * @return picture 的封装对象
     */
    @Override
    public PictureVO uploadPicture(Object object, PictureUploadRequest pictureUploadRequest, User loginUser) {
        checkParam(pictureUploadRequest, loginUser, object);
        Long pictureId = pictureUploadRequest.getId();
        PictureUploadTemplate uploadTemplate = uploadFactory.getUploadFactory(object instanceof MultipartFile ? FileUploadEnum.FILE.getType() : FileUploadEnum.URL.getType());
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = uploadTemplate.uploadPicture(object, uploadPathPrefix);
        Picture picture = new Picture();
        if (pictureId == null) {
            // 新增
            picture = buildPicture(uploadPictureResult, null, loginUser.getId());
        }
        if (pictureId != null) {
            // 更新
            Picture oldPicture = pictureDAO.getByPictureId(pictureId);
            ThrowUtils.throwIf(ObjectUtils.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 校验权限 如果不是自己或者 admin 就抛出异常
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            picture = buildPicture(uploadPictureResult, oldPicture, loginUser.getId());
        }
        // 构造要入库的图片信息
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 更新或者插入
        ThrowUtils.throwIf(!pictureDAO.saveOrUpdate(picture), ErrorCode.SYSTEM_ERROR, "保存失败");
        return PictureVO.objToVo(picture);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }


    private Picture buildPicture(UploadPictureResult result, Picture oldPicture, Long userId) {
        return Picture.builder()
                .id(oldPicture != null ? oldPicture.getId() : null)
                .editTime(oldPicture != null ? new Date() : null)
                .url(result.getUrl())
                .name(result.getPicName())
                .picScale(result.getPicScale())
                .picFormat(result.getPicFormat())
                .picHeight(result.getPicHeight())
                .picWidth(result.getPicWidth())
                .userId(userId)
                .picSize(result.getPicSize())
                .build();
    }

    private void checkParam(PictureUploadRequest pictureUploadRequest, User loginUser, Object object) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (pictureUploadRequest == null || object == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 增加支持审核字段，进行搜索
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userDAO.getById(userId);
            UserVO userVO = UserVO.objToVo(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userDAO.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(UserVO.objToVo(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        ThrowUtils.throwIf(pictureReviewRequest == null || loginUser == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        Long id = pictureReviewRequest.getId();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        Integer reviewStatus = PictureReviewStatusEnum.getEnumByValue(pictureReviewRequest.getReviewStatus()).getValue();
        Picture oldPicture = pictureDAO.getByPictureId(id);
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "图片已审核");
        ThrowUtils.throwIf(ObjectUtils.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        Picture updatePicture = buildUpdateReviewPicture(loginUser, oldPicture, reviewStatus, reviewMessage);
        ThrowUtils.throwIf(!pictureDAO.updateById(updatePicture), ErrorCode.SYSTEM_ERROR, "审核失败");
    }

    private Picture buildUpdateReviewPicture(User loginUser, Picture oldPicture, Integer reviewStatus, String reviewMessage) {
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(oldPicture, updatePicture);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewMessage(reviewMessage);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        return updatePicture;
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
}




