package com.leikooo.yupicturebackend.manager.auth;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.spring.pathmatch.SaPatternsRequestConditionHolder;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.leikooo.yupicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.leikooo.yupicturebackend.model.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Slf4j
public class SaTokenFilterAuthStrategy {

    public SaFilterAuthStrategy getSaFilterAuthStrategy() {
        return obj -> {
            // 登录校验：拦截所有路由，排除登录接口
            if (SaRouter.match("/api/user/login", "/api/user/register").isHit) {
                return;
            }
            SaRouter.match("/**", r -> StpKit.SPACE.checkLogin());

            // 获取用户ID和权限上下文
            Long userId = ((User) StpKit.SPACE.getSession().get(USER_LOGIN_STATE)).getId();
            SpaceUserAuthContext spaceUserAuthContext = (SpaceUserAuthContext) SaTokenContextHolder.get(
                    Objects.nonNull(userId) ? userId.toString() : ""
            );

            // 动态权限路由配置
            Map<String, String[][]> routePermissionMap = getRoutePermissionMap();
            String requestPath = SaHolder.getRequest().getRequestPath();

            // 匹配路由并校验权限
            for (Map.Entry<String, String[][]> entry : routePermissionMap.entrySet()) {
                if (SaPatternsRequestConditionHolder.match(entry.getKey(), requestPath)) {
                    checkPermission(spaceUserAuthContext, entry.getValue(), userId);
                    return; // 匹配到路由后终止
                }
            }
        };
    }

    /**
     * 定义路由和权限关键字的对应关系
     */
    private Map<String, String[][]> getRoutePermissionMap() {
        Map<String, String[][]> map = new HashMap<>();
        map.put("/api/picture/**", new String[][]{
                {"edit", SpaceUserPermissionConstant.PICTURE_EDIT},
                {"delete", SpaceUserPermissionConstant.PICTURE_DELETE},
                {"upload", SpaceUserPermissionConstant.PICTURE_UPLOAD},
                {"admin", SpaceUserPermissionConstant.SPACE_USER_MANAGE},
                {"view", SpaceUserPermissionConstant.PICTURE_VIEW}
        });
        map.put("/api/spaceUser/**", new String[][]{
                {"", SpaceUserPermissionConstant.SPACE_USER_MANAGE}
        });
        return map;
    }

    /**
     * 检查用户权限
     *
     * @param authContext 用户权限上下文
     * @param rules       权限规则（关键词与权限的映射）
     */
    private void checkPermission(SpaceUserAuthContext authContext, String[][] rules, Long userId) {
        String requestPath = SaHolder.getRequest().getRequestPath();
        for (String[] rule : rules) {
            String keyword = rule[0];
            String permission = rule[1];
            if (requestPath.contains(keyword) &&
                    !StpKit.SPACE.hasElement(authContext.getPermissionList(), permission)) {
                log.error("Sa-Token 无权限，路径: {}, 用户ID: {}", SaHolder.getRequest().getRequestPath(), userId);
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "Sa-Token 无权限");
            }
        }
    }
}
