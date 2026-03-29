package com.zqw.qwpicturebackend.websocket.config;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.zqw.qwpicturebackend.auth.SpaceUserAuthManager;
import com.zqw.qwpicturebackend.auth.constant.SpaceUserPermissionConstant;
import com.zqw.qwpicturebackend.exception.BusinessException;
import com.zqw.qwpicturebackend.exception.ErrorCode;
import com.zqw.qwpicturebackend.model.entity.Picture;
import com.zqw.qwpicturebackend.model.entity.Space;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.enums.SpaceTypeEnum;
import com.zqw.qwpicturebackend.service.PictureService;
import com.zqw.qwpicturebackend.service.SpaceService;
import com.zqw.qwpicturebackend.service.SpaceUserService;
import com.zqw.qwpicturebackend.service.UserService;
import com.zqw.qwpicturebackend.websocket.model.PictureEditActionEnum;
import com.zqw.qwpicturebackend.websocket.model.PictureEditMessageTypeEnum;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 有websocket请求连接前，拦截住，做一些操作  1. 权限校验   2. 参数构造
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            @NotNull Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            // 1. 获取参数
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 图片校验
            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.info("图片id为空，拒绝建连");
                return false;
            }

            User loginUser = userService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.info("用户id为空，拒绝建连");
                return false;
            }

            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.info("非数据库所有图片，拒绝建连");
                return false;
            }

            // 空间校验
            Long spaceId = picture.getSpaceId();
            if (spaceId == null) {
                log.info("图片归属空间id不存在，拒绝建连");
                return false;
            }
            Space space = spaceService.getById(spaceId);
            if (space == null || !space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                log.info("空间图片不合法无法建连 || 只有团队空间才有资格建连");
                return false;
            }
            // 进行空间用户权限校验
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.info("你没有权限编辑当前团队空间图片");
                return false;
            }
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());  // 当前用户
            attributes.put("pictureId", Long.valueOf(pictureId));    // 记得转为Long todo 还是没有转😄
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
