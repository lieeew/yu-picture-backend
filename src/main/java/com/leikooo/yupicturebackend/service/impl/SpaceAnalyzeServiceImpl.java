package com.leikooo.yupicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.dao.SpaceDAO;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.mapper.PictureMapper;
import com.leikooo.yupicturebackend.mapper.SpaceMapper;
import com.leikooo.yupicturebackend.model.dto.space.analyze.*;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.model.entity.Space;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.enums.TimeDimensionEnum;
import com.leikooo.yupicturebackend.model.vo.space.analyze.*;
import com.leikooo.yupicturebackend.service.SpaceAnalyzeService;
import com.leikooo.yupicturebackend.service.SpaceService;
import com.leikooo.yupicturebackend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/1/10
 * @description
 */
@AllArgsConstructor
@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    private final UserService userService;

    private final SpaceDAO spaceDAO;

    private final PictureDAO pictureDAO;

    private final SpaceService spaceService;

    private final PictureMapper pictureMapper;

    private final SpaceMapper spaceMapper;


    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 全空间分析或者公共图库权限校验：仅管理员可访问
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 分析特定空间，仅本人或管理员可以访问
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceDAO.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(Objects.isNull(spaceCategoryAnalyzeRequest), ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        Map<String, Object> params = fillAnalyzeQueryMap(spaceCategoryAnalyzeRequest);
        List<Map<String, Object>> categoryStatistics = pictureMapper.getCategoryStatistics(params);
        return categoryStatistics.stream().filter(Objects::nonNull).map(map -> {
            Long count = ((Number) map.get("count")).longValue();
            Long totalSize = ((Number) map.get("totalSize")).longValue();
            String category = map.get("category") instanceof String ? (String) map.get("category") : "未分类";
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).toList();
    }


    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest SpaceUsageAnalyzeRequest 请求参数
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse 分析结果
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.select("picSize");
        fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, pictureQueryWrapper);
        // 只查询 picSize 字段优化查询速度
        List<Object> pictures = pictureDAO.getBaseMapper().selectObjs(pictureQueryWrapper);
        // 当前图片数量
        long pictureCount = pictures.size();
        // 已使用大小
        long pictureUsages = pictures.stream().filter(Objects::nonNull)
                .mapToLong(res -> res instanceof Long ? (Long) res : 0L).sum();
        // 公共图片没有比例、上线这些参数
        if (spaceUsageAnalyzeRequest.isQueryPublic() || spaceUsageAnalyzeRequest.isQueryAll()) {
            return SpaceUsageAnalyzeResponse.builder()
                    .usedSize(pictureCount)
                    .usedCount(pictureUsages).build();
        }
        // 分析某个 space 空间
        if (Objects.nonNull(spaceUsageAnalyzeRequest.getSpaceId()) && spaceUsageAnalyzeRequest.getSpaceId() > 0L) {
            Space spaceInfo = spaceDAO.getSpaceByUserIdAndSpaceId(spaceUsageAnalyzeRequest.getSpaceId(), loginUser.getId());
            ThrowUtils.throwIf(spaceInfo == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 权限校验：仅空间所有者或管理员可访问
            spaceService.checkSpaceAuth(loginUser, spaceInfo);
            // 构造返回信息
            double sizeUsageRatio = NumberUtil.round(spaceInfo.getTotalSize() * 100.0 / spaceInfo.getMaxSize(), 2).doubleValue();
            double countUsageRatio = NumberUtil.round(spaceInfo.getTotalCount() * 100.0 / spaceInfo.getMaxCount(), 2).doubleValue();
            return SpaceUsageAnalyzeResponse.builder()
                    .maxCount(spaceInfo.getMaxCount())
                    .maxSize(spaceInfo.getMaxSize())
                    .countUsageRatio(countUsageRatio)
                    .sizeUsageRatio(sizeUsageRatio)
                    .usedSize(pictureCount)
                    .usedCount(pictureUsages)
                    .build();
        }
        return new SpaceUsageAnalyzeResponse();
    }

    /**
     * 填充 QueryWrapper 对应属性
     *
     * @param spaceAnalyzeRequest spaceAnalyzeRequest 请求类
     * @param queryWrapper        queryWrapper 查询对象
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        }
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        // 查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureDAO.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjUtil::isNotNull).map(Object::toString).toList();
        // 合并所有标签并统计使用次数
        // 这个 flatMap 就是把 ["a", "b"] ["c", "d"] 转换为 ["a", "b", "c", "d"] 把他拍扁的感觉
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 转换为响应对象，按使用次数降序排序
        return tagCountMap.entrySet().stream()
                // 降序排列
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间大小分析
     *
     * @param spaceSizeAnalyzeRequest SpaceSizeAnalyzeRequests
     * @param loginUser               当前登录用户
     * @return List<SpaceSizeAnalyzeResponse>
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        // 定义分段范围，注意使用有序 Map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        pictureDAO.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .forEach(picSize -> {
                    if (picSize < 100 * 1024) {
                        sizeRanges.put("<100KB", sizeRanges.getOrDefault("<100KB", 0L) + 1);
                    } else if (picSize < 500 * 1024) {
                        sizeRanges.put("100KB-500KB", sizeRanges.getOrDefault("100KB-500KB", 0L) + 1);
                    } else if (picSize < 1024 * 1024) {
                        sizeRanges.put("500KB-1MB", sizeRanges.getOrDefault("500KB-1MB", 0L) + 1);
                    } else {
                        sizeRanges.put(">1MB", sizeRanges.getOrDefault(">1MB", 0L) + 1);
                    }
                });
        // 转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        String timeDimensionValue = TimeDimensionEnum.getEnumByValue(spaceUserAnalyzeRequest.getTimeDimension()).getValue();
        Map<String, Object> params = fillAnalyzeQueryMap(spaceUserAnalyzeRequest);
        params.put("timeDimension", timeDimensionValue);
        List<Map<String, Object>> queryResult = pictureMapper.analyzeByTimeDimension(params);
        // 查询结果并转换
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> fillAnalyzeQueryMap(SpaceAnalyzeRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("queryAll", request.isQueryAll());
        params.put("queryPublic", request.isQueryPublic());
        params.put("spaceId", request.getSpaceId());
        return params;
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");
        return spaceMapper.getTopNSpaceUsage(spaceRankAnalyzeRequest.getTopN());
    }

}


