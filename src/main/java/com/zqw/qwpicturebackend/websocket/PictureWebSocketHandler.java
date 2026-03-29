package com.zqw.qwpicturebackend.websocket;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.zqw.qwpicturebackend.model.entity.User;
import com.zqw.qwpicturebackend.model.vo.UserVO;
import com.zqw.qwpicturebackend.service.UserService;
import com.zqw.qwpicturebackend.websocket.model.PictureEditActionEnum;
import com.zqw.qwpicturebackend.websocket.model.PictureEditMessageTypeEnum;
import com.zqw.qwpicturebackend.websocket.model.PictureEditRequestMessage;
import com.zqw.qwpicturebackend.websocket.model.PictureEditResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PictureWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    // picture <--1对1--> userId
    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUser = new ConcurrentHashMap<>();

    // group
    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();


    /**
     * 建连，不仅需要添加到 group中，还要为每个图片设置唯一操作人
     *
     * @param session 等待建立连接的session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 获取参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 2. 添加到 group中
        pictureSessions.putIfAbsent(pictureId, new HashSet<>());
        pictureSessions.get(pictureId).add(session);


        // 3. 创建本人添加到编辑中的广播
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("%s，进入当前团队空间中", user.getUserName()));
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 4. 广播消息。非action操作都要广播
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 捕获到浏览器发送的请求 【前端请求】         active浏览器  ---->  websocket
     * handleTextMessage 仅作为 “接收消息” 的入口，“发送消息” 是独立的输出操作，直接下发到对应浏览器
     *
     * @param session 本次浏览器向websocket通信的session
     * @param message 告诉当前session需要执行什么操作
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 1. 获取操作类型
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 2. 需要处理的操作
        PictureEditRequestMessage pictureEditMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT -> {
                // 进入编辑
                handleEnterEditMessage(pictureEditMessage, session, user, pictureId);
            }
            case EDIT_ACTION -> {
                // 执行编辑
                handleActionEditMessage(pictureEditMessage, session, user, pictureId);
            }
            case EXIT_EDIT -> {
                // 退出编辑
                handleExitEditMessage(pictureEditMessage, session, user, pictureId);
            }
            default -> {
                // 发送广播告诉其错误信息
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误，导致我无法处理别人的数据包");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                // 发给前端
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
            }
        }

    }

    /**
     * 用户退出编辑，需要将 pictureEditingUser 中当前图片清空  todo 正常退出，如果 websocket连接直接断了呢？还是要将当前用户踢出去
     *  todo: 注意退出编辑后，并不是代表一定要将该用户从group中删除，而是 websocket连接断开才将用户踢出去
     * @param pictureEditMessage
     * @param session
     * @param user
     * @param pictureId
     */
    private void handleExitEditMessage(PictureEditRequestMessage pictureEditMessage,
                                       WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUser.get(pictureId);
        // 只有当前用户是当前编辑者才能删除
        if (ObjectUtils.isNotEmpty(editingUserId) && editingUserId.equals(user.getId())) {
            // 删除正在编辑map
            pictureEditingUser.remove(pictureId);

            // 下发消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("编辑者%s, 退出编辑", user.getUserName()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }


    /**
     * 用户进行了具体的编辑操作，需要将操作下发到同一个group中的其他用户
     *
     * @param pictureEditMessage
     * @param session
     * @param user
     * @param pictureId
     * @throws IOException
     */
    private void handleActionEditMessage(PictureEditRequestMessage pictureEditMessage,
                                         WebSocketSession session, User user, Long pictureId) throws IOException {
        // 正在操作图片的用户id
        Long editingUserId = pictureEditingUser.get(pictureId);
        // 执行的图片操作
        String editAction = pictureEditMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            log.info("当前编辑者没有进行任何操作");
            return;
        }
        // 是否能够编辑
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 构造编辑结果
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setMessage(String.format("当前用户：%s，正在执行：%s操作", user.getUserName(), actionEnum.getText()));
            pictureEditResponseMessage.setEditAction(actionEnum.getValue());
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 本人操作完了，这里只是为了下发一下自己的操作，所以自己不需要再次操作
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理进入编辑的消息 【】
     *
     * @param pictureEditMessage
     * @param session
     * @param user
     * @param pictureId
     */
    private void handleEnterEditMessage(PictureEditRequestMessage pictureEditMessage, WebSocketSession session,
                                        User user, Long pictureId) throws Exception {
        // 判断当前用户是否能够操作
        if (!pictureEditingUser.containsKey(pictureId)) {
            // 将当前用户设置为正在编辑图片的用户
            pictureEditingUser.put(pictureId, user.getId());
            // 构造下发消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("用户：{%s}开始编辑图片", user.getUserName()));
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 发送消息
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 当关闭websocket连接时需要清除对应 group 的 session
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 关闭 websocket 时需要清除对应的组中session
        // 1. 获取参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // todo 如果用户正在编辑，突然 websocket连接断开了，此时需要让出 pictureEditingUser
        handleExitEditMessage(null, session, user, pictureId);

        // 2. 删除对应 group中对应的 member
        boolean remove = pictureSessions.get(pictureId).remove(session);
        if (!remove) {
            log.info("删除团队空间中的session失败");
        }
        if (pictureSessions.get(pictureId).isEmpty()) {
            pictureSessions.remove(pictureId);
        }

        // 3. 构造响应消息
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("%s, 退出了当前团队空间", user.getId()));
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        // 4. 发送消息
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }


    // 全部广播  【session.sendMessage() 直接将响应信息发给浏览器】  调用方 --->  目标浏览器
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    /**
     * 该方法用来广播消息到对应的组中，因为是直接通过 websocket进行响应
     *
     * @param pictureId                  组中的key
     * @param excludeSession             自己进行过的操作需要发送给其他session，但是自己就不用再重复接收消息并操作
     * @param pictureEditResponseMessage 需要广播的消息
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession)
            throws IOException {
        // 1. 获取 session列表
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(webSocketSessions)) {
            // 构造 WebSocketMessage 这是ws特殊的请求
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化
            // 将Long转为 String 解决精度丢失问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);   // 支持
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);    // 支持Long基本类型
            objectMapper.registerModule(module);
            String text = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(text);
            // 拿到对应的 session 发送信息
            for (WebSocketSession session : webSocketSessions) {
                if (session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }
}
