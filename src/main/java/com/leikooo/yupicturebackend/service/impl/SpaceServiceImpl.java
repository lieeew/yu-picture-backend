package com.leikooo.yupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.dao.SpaceDAO;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.enums.SpaceLevelEnum;
import com.leikooo.yupicturebackend.model.vo.SpaceVO;
import com.leikooo.yupicturebackend.service.SpaceService;
import com.leikooo.yupicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
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


    public SpaceServiceImpl(TransactionTemplate transactionTemplate, UserService userService, SpaceDAO spaceDAO) {
        this.transactionTemplate = transactionTemplate;
        this.userService = userService;
        this.spaceDAO = spaceDAO;
    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1）填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        space.setUserId(loginUser.getId());
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




