package com.leikooo.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.dao.SpaceDAO;
import com.leikooo.yupicturebackend.dao.SpaceUserDAO;
import com.leikooo.yupicturebackend.dao.UserDAO;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.manager.auth.SaTokenContextHolder;
import com.leikooo.yupicturebackend.manager.auth.SpaceUserAuthContext;
import com.leikooo.yupicturebackend.manager.sharding.DynamicShardingManager;
import com.leikooo.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceDeleteRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.SpaceUser;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.enums.SpaceLevelEnum;
import com.leikooo.yupicturebackend.model.enums.SpaceRoleEnum;
import com.leikooo.yupicturebackend.model.enums.SpaceTypeEnum;
import com.leikooo.yupicturebackend.model.vo.SpaceVO;
import com.leikooo.yupicturebackend.model.vo.UserVO;
import com.leikooo.yupicturebackend.service.PictureService;
import com.leikooo.yupicturebackend.service.SpaceService;
import com.leikooo.yupicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author leikooo
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @date 2024-12-26 23:54:29
 */
@Service
@Slf4j
@AllArgsConstructor
public class SpaceServiceImpl implements SpaceService {

    private final TransactionTemplate transactionTemplate;

    private final UserService userService;

    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    private final SpaceDAO spaceDAO;

    private final PictureDAO pictureDAO;

    private final PictureService pictureService;

    private final UserDAO userDAO;

    private final SpaceUserDAO spaceUserDAO;

    // private final DynamicShardingManager dynamicShardingManager;

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
                    boolean isExist = spaceDAO.isExistByUserIdAndType(loginUser.getId(), space.getSpaceType());
                    ThrowUtils.throwIf(isExist, ErrorCode.SYSTEM_ERROR, "用户已存在私有空间");
                    ThrowUtils.throwIf(!spaceDAO.save(space), ErrorCode.SYSTEM_ERROR, "创建失败");
                    if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                        SpaceUser spaceUser = SpaceUser.builder()
                                .userId(space.getUserId())
                                .spaceRole(SpaceRoleEnum.ADMIN.getValue())
                                .spaceId(space.getId())
                                .build();
                        ThrowUtils.throwIf(!spaceUserDAO.save(spaceUser), ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                    }
                    // dynamicShardingManager.createSpacePictureTable(space);
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
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    private Space buildInsertSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        BeanUtils.copyProperties(spaceAddRequest, space);
        space.setUserId(loginUser.getId());
        this.fillSpaceBySpaceLevel(space);
        return space;
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 要创建
        if (add) {
            if (StringUtils.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            // 修改数据时，如果要改空间级别
            if (spaceType != null && spaceTypeEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
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
     *
     * @param spaceId   spaceId
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
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        User user = userService.getLoginUser(request);
        List<String> permissionList = ((SpaceUserAuthContext) SaTokenContextHolder.get(user.getId().toString())).getPermissionList();
        spaceVO.setPermissionList(permissionList);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User spaceUser = userDAO.getById(userId);
            UserVO userVO = userService.getUserVO(spaceUser);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userDAO.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
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




