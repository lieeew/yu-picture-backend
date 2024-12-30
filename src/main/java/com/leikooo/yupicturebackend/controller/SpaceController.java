package com.leikooo.yupicturebackend.controller;

import com.leikooo.yupicturebackend.annotation.AuthCheck;
import com.leikooo.yupicturebackend.commen.BaseResponse;
import com.leikooo.yupicturebackend.commen.ResultUtils;
import com.leikooo.yupicturebackend.dao.SpaceDAO;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.model.constant.UserConstant;
import com.leikooo.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceDeleteRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceUpdateRequest;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.service.SpaceService;
import com.leikooo.yupicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/15
 * @description
 */
@Slf4j
@RestController
@RequestMapping("/space")
@AllArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    private final SpaceDAO spaceDAO;

    private final UserService userService;

    /**
     * 更新接口
     * @param spaceUpdateRequest 前端请求
     * @return Boolean true 是成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(
            @RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        ThrowUtils.throwIf(spaceUpdateRequest == null || spaceUpdateRequest.getId() < 0,
                ErrorCode.PARAMS_ERROR, "参数错误");
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        spaceService.validSpace(space, false);
        spaceService.fillSpaceBySpaceLevel(space);
        Long spaceId = space.getId();
        Space originSpace = spaceDAO.getById(spaceId);
        ThrowUtils.throwIf(originSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(!spaceDAO.updateById(space), ErrorCode.SYSTEM_ERROR, "更新失败");
        return ResultUtils.success(true);
    }

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody SpaceDeleteRequest spaceDeleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceDeleteRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!spaceService.deleteSpace(spaceDeleteRequest, loginUser), ErrorCode.SYSTEM_ERROR, "删除失败");
        return ResultUtils.success(true);
    }
}
