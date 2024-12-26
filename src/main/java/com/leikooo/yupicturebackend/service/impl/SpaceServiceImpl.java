package com.leikooo.yupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leikooo.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.leikooo.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.vo.SpaceVO;
import com.leikooo.yupicturebackend.service.SpaceService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

/**
 * @author leikooo
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @date 2024-12-26 23:54:29
 */
@Service
@Slf4j
@AllArgsConstructor
public class SpaceServiceImpl implements SpaceService {

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        return 0;
    }

    @Override
    public void validSpace(@NonNull Space space, boolean add) {
        System.out.println(space);
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

    }
}




