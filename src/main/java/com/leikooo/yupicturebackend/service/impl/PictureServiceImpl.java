package com.leikooo.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.leikooo.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.leikooo.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.dao.SpaceDAO;
import com.leikooo.yupicturebackend.dao.UserDAO;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.manager.CosManager;
import com.leikooo.yupicturebackend.manager.auth.SaTokenContextHolder;
import com.leikooo.yupicturebackend.manager.auth.SpaceUserAuthContext;
import com.leikooo.yupicturebackend.manager.factory.UploadFactory;
import com.leikooo.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.leikooo.yupicturebackend.model.dto.file.UploadPictureResult;
import com.leikooo.yupicturebackend.model.dto.picture.*;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.Urls;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.enums.FileUploadEnum;
import com.leikooo.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.leikooo.yupicturebackend.model.vo.PictureVO;
import com.leikooo.yupicturebackend.model.vo.UserVO;
import com.leikooo.yupicturebackend.service.PictureService;
import com.leikooo.yupicturebackend.service.UserService;
import com.leikooo.yupicturebackend.utils.ColorSimilarUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author liang
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @date 2024-12-16 11:56:07
 */
@Slf4j
@Service
@AllArgsConstructor
public class PictureServiceImpl implements PictureService {

    private final PictureDAO pictureDAO;

    private final UserDAO userDAO;

    private final UserService userService;

    private final UploadFactory uploadFactory;

    private final SpaceDAO spaceDAO;

    private final CosManager cosManager;

    private final TransactionTemplate transactionTemplate;

    private final Executor yuPictureExecutor;

    private final AliYunAiApi aliYunAiApi;

    /**
     * 上传图片接口
     *
     * @param object               文件/url
     * @param pictureUploadRequest 请求类
     * @return picture 的封装对象
     */
    @Override
    public PictureVO uploadPicture(Object object, PictureUploadWithUserDTO pictureUploadRequest) {
        checkParam(pictureUploadRequest, object);
        User loginUser = pictureUploadRequest.getUser();
        Long spaceId = getSpaceIdAndCheckAuth(pictureUploadRequest);
        checkSpaceUsage(spaceId);
        Long pictureId = pictureUploadRequest.getId();
        PictureUploadTemplate uploadTemplate = uploadFactory.getUploadFactory(object instanceof MultipartFile ? FileUploadEnum.FILE.getType() : FileUploadEnum.URL.getType());
        String uploadPathPrefix = Objects.nonNull(spaceId) ? String.format("space/%s", spaceId) : String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = uploadTemplate.uploadPicture(object, uploadPathPrefix);
        Picture picture = new Picture();
        if (pictureId == null) {
            // 新增
            picture = buildPicture(uploadPictureResult, null, pictureUploadRequest);
        }
        if (pictureId != null) {
            // 更新
            Picture updatePicture = buildUpdatePicture(loginUser, pictureId, spaceId);
            picture = buildPicture(uploadPictureResult, updatePicture, pictureUploadRequest);
        }
        // 如果 spaceId 是 null 就是 null
        picture.setSpaceId(picture.getSpaceId());
        // 补充审核参数 空间图片不需要校验
        this.fillReviewParams(picture, loginUser);
        // 更新或者插入
        final Picture finalPicture = picture;
        transactionTemplate.execute((status) -> {
            ThrowUtils.throwIf(!pictureDAO.saveOrUpdate(finalPicture), ErrorCode.SYSTEM_ERROR, "保存失败");
            if (Objects.nonNull(spaceId)) {
                ThrowUtils.throwIf(!spaceDAO.updateUsage(spaceId, finalPicture.getPicSize()), ErrorCode.SYSTEM_ERROR, "额度更新失败");
            }
            return null;
        });
        return PictureVO.objToVo(picture);
    }

    private void checkSpaceUsage(Long spaceId) {
        if (spaceId != null) {
            Space space = spaceDAO.getById(spaceId);
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
    }

    /**
     * 是否上传到 space 空间 & 校验是否有权限
     *
     * @param pictureUploadRequest dto 封装类 包含 user
     * @return spaceId 如果是 null 就表示不上传到 space
     */
    private Long getSpaceIdAndCheckAuth(PictureUploadWithUserDTO pictureUploadRequest) {
        User loginUser = pictureUploadRequest.getUser();
        Long spaceId = pictureUploadRequest.getSpaceId();
        Long spaceIdOfPicture = null;
        // 校验空间是否存在并且是否有权限上传/修改
        if (spaceId != null) {
            Space space = spaceDAO.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传，这个管理员指的是创建空间人
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "空间创建人才能上传图片");
            spaceIdOfPicture = spaceId;
        }
        if (Objects.isNull(spaceIdOfPicture) && pictureUploadRequest.getId() != null) {
            Picture picture = pictureDAO.getByPictureId(pictureUploadRequest.getId());
            ThrowUtils.throwIf(Objects.isNull(picture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (Objects.isNull(picture.getSpaceId())) {
                return null;
            }
            spaceIdOfPicture = picture.getSpaceId();
        }
        return spaceIdOfPicture;
    }

    private Picture buildUpdatePicture(User loginUser, Long pictureId, Long spaceId) {
        Picture oldPicture = pictureDAO.getByPictureId(pictureId);
        ThrowUtils.throwIf(ObjectUtils.isEmpty(oldPicture), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验权限 如果不是自己或者 admin 就抛出异常
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 校验空间
        if (Objects.isNull(spaceId) && Objects.nonNull(oldPicture.getSpaceId())) {
            return oldPicture;
        }
        if (Objects.nonNull(spaceId) && !Objects.equals(spaceId, oldPicture.getSpaceId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不匹配");
        }
        return oldPicture;
    }

    private Picture buildPicture(UploadPictureResult result, Picture oldPicture, PictureUploadWithUserDTO pictureUploadWithUserDTO) {
        return Picture.builder()
                .id(oldPicture != null ? oldPicture.getId() : null)
                .editTime(oldPicture != null ? new Date() : null)
                .spaceId(oldPicture != null && oldPicture.getSpaceId() != null || pictureUploadWithUserDTO.getSpaceId() != null ? oldPicture == null ? pictureUploadWithUserDTO.getSpaceId() : oldPicture.getSpaceId() : null)
                .urls(result.getUrls())
                .picColor(result.getPicColor())
                .name(StringUtils.isNotBlank(pictureUploadWithUserDTO.getPicName()) ? pictureUploadWithUserDTO.getPicName() : result.getPicName())
                .picScale(result.getPicScale())
                .picFormat(result.getPicFormat())
                .picHeight(result.getPicHeight())
                .picWidth(result.getPicWidth())
                .userId(pictureUploadWithUserDTO.getUser().getId())
                .picSize(result.getPicSize())
                .build();
    }


    private void checkParam(PictureUploadWithUserDTO pictureUploadRequest, Object object) {
        if (pictureUploadRequest == null || object == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
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
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest.toPictureUploadWithUserDTO(loginUser));
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
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
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
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
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
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
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = ((SpaceUserAuthContext) SaTokenContextHolder.get(loginUser.getId().toString())).getPermissionList();
        pictureVO.setPermissionList(permissionList.stream().filter(r -> r.contains("picture")).toList());
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
        String url = Optional.of(picture).map(Picture::getUrls).map(Urls::getUrl).orElse("");
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

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = pictureDAO.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            ThrowUtils.throwIf(!pictureDAO.removeById(pictureId), ErrorCode.OPERATION_ERROR);
            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                ThrowUtils.throwIf(!spaceDAO.delPictureUpdateSpaceUsage(spaceId, oldPicture.getPicSize()),
                        ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return null;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断改图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrls().getUrl();
        long count = pictureDAO.getOneUrlCount(pictureUrl);
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        Urls urls = oldPicture.getUrls();
        if (urls != null) {
            List<String> delKeys = Stream.of(
                            urls.getOriginalUrl(),
                            urls.getUrl(),
                            urls.getThumbnailUrl(),
                            urls.getTransferUrl()
                    )
                    // 过滤非空字段
                    .filter(StrUtil::isNotBlank)
                    // 转换为删除键
                    .map(this::getPictureKey)
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(delKeys)) {
                cosManager.deleteObjects(delKeys);
            }
        }
    }

    /**
     * 需要获取 public/1867564572769492994/2024-12-28_xxx_xxx.webp 才能删除 cos 里面的数据
     * <a href="https://xxx.xxxx.cos.ap-beijing.myqcloud.com/public/1867564572769492994/2024-12-28_xxx_xxx.webp">...</a>
     *
     * @param pictureUrl url
     * @return key
     */
    private String getPictureKey(String pictureUrl) {
        return pictureUrl.replace(cosManager.getBaseUrl() + "/", "");
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        if (spaceId != null) {
            // 私有空间，仅空间管理员(创建者)可操作/查看
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureDAO.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureDAO.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceDAO.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询该空间下所有图片（必须有主色调）
        List<Picture> pictureList = pictureDAO.queryHasColor(spaceId);
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 将目标颜色转为 Color 对象
        Color targetColor = Color.decode(picColor);
        // 4. 计算相似度并排序
        List<Picture> sortedPictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    // 提取图片主色调
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片放到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                // 取前 12 个
                .limit(12)
                .toList();

        // 转换为 PictureVO
        return sortedPictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void batchEditPictureMetadata(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1. 获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceDAO.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询指定图片（仅选择需要的字段）
        List<Picture> pictureList = pictureDAO.getPictureByIds(pictureIdList, spaceId);
        if (pictureList.isEmpty()) {
            return;
        }
        // 4. 更新分类和标签
        // 一次处理 100 个
        int batchSize = 100;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < pictureList.size(); i += batchSize) {
            List<Picture> subList = pictureList.subList(i, Math.min(i + batchSize, pictureList.size()));
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                subList.forEach(picture -> {
                    if (StrUtil.isNotBlank(category)) {
                        picture.setCategory(category);
                    }
                    if (CollUtil.isNotEmpty(tags)) {
                        picture.setTags(JSONUtil.toJsonStr(tags));
                    }
                });
                String nameRule = pictureEditByBatchRequest.getNameRule();
                this.fillPictureWithNameRule(pictureList, nameRule);
                ThrowUtils.throwIf(!pictureDAO.updateBatchById(subList), ErrorCode.OPERATION_ERROR, "批量编辑失败");
            }, yuPictureExecutor);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        // 5. 操作数据库进行批量更新
        boolean result = pictureDAO.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败");
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(pictureDAO.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验
        checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrls().getOriginalUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

}




