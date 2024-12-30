package com.leikooo.yupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.dao.SpaceDAO;
import com.leikooo.yupicturebackend.dao.UserDAO;
import com.leikooo.yupicturebackend.event.SpaceDelEvent;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceDeleteRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.enums.SpaceLevelEnum;
import com.leikooo.yupicturebackend.model.vo.SpaceVO;
import com.leikooo.yupicturebackend.service.PictureService;
import com.leikooo.yupicturebackend.service.SpaceService;
import com.leikooo.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author leikooo
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @date 2024-12-26 23:54:29
 */
@Service
@Slf4j
public class SpaceServiceImpl implements SpaceService {

    private final TransactionTemplate transactionTemplate;

    private final UserService userService;

    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    private final SpaceDAO spaceDAO;

    private final PictureDAO pictureDAO;

    private final PictureService pictureService;

    private final ApplicationEventPublisher eventPublisher;

    public SpaceServiceImpl(TransactionTemplate transactionTemplate, UserService userService, SpaceDAO spaceDAO, PictureDAO pictureDAO, PictureService pictureService, ApplicationEventPublisher eventPublisher) {
        this.transactionTemplate = transactionTemplate;
        this.userService = userService;
        this.spaceDAO = spaceDAO;
        this.pictureDAO = pictureDAO;
        this.pictureService = pictureService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1）填充参数默认值
        Space space = buildInsertSpace(spaceAddRequest, loginUser);
        //2）校验参数
        validSpace(space, true);
        //3）校验权限，非管理员只能创建普通级别的空间
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (!userService.isAdmin(loginUser) && spaceLevelEnum != SpaceLevelEnum.COMMON) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建此级别空间");
        }
        //4）同一个账号自能创建一个私有空间
        Object lock = lockMap.computeIfAbsent(loginUser.getId(), k -> new Object());
        synchronized (lock) {
            try {
                //5）操作数据库
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean isExist = spaceDAO.isExistSpaceByUserId(loginUser.getId());
                    ThrowUtils.throwIf(isExist, ErrorCode.SYSTEM_ERROR, "用户已存在私有空间");
                    ThrowUtils.throwIf(!spaceDAO.save(space), ErrorCode.SYSTEM_ERROR, "创建失败");
                    return space.getId();
                });
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                // 移除
                lockMap.remove(loginUser.getId());
            }
        }
    }

    private Space buildInsertSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        space.setUserId(loginUser.getId());
        fillSpaceBySpaceLevel(space);
        return space;
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StringUtils.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        if (StringUtils.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    @Override
    public boolean deleteSpace(SpaceDeleteRequest spaceDeleteRequest, User loginUser) {
        final Long spaceId = spaceDeleteRequest.getSpaceId();
        validDeleteSpace(spaceId, loginUser);
        transactionTemplate.execute((status -> {
            // 删除空间
            ThrowUtils.throwIf(!spaceDAO.removeById(spaceId), ErrorCode.OPERATION_ERROR, "删除失败");
            // 删除相关图片
            List<Picture> pictures = pictureDAO.getListPageBySpaceId(spaceId, loginUser.getId());
            if (!CollectionUtils.isEmpty(pictures)) {
                pictureDAO.deleteBatchIds(pictures.stream().map(Picture::getId).toList());
                pictures.forEach(pictureService::clearPictureFile);
            }
            return null;
        }));
        return true;
    }

    /**
     * 校验逻辑是否允许删除空间
     * @param spaceId spaceId
     * @param loginUser 登录用户
     */
    private void validDeleteSpace(Long spaceId, User loginUser) {
        if (spaceId == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = spaceDAO.getSpaceByUserIdAndSpaceId(spaceId, loginUser.getId());
        if (Objects.isNull(space) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "删除空间失败");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        return null;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        return null;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return null;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        long maxSize = spaceLevelEnum.getMaxSize();
        if (space.getMaxSize() == null) {
            space.setMaxSize(maxSize);
        }
        long maxCount = spaceLevelEnum.getMaxCount();
        if (space.getMaxCount() == null) {
            space.setMaxCount(maxCount);
        }
    }
}




