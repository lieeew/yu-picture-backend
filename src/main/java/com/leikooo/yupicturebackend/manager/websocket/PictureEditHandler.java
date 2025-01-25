package com.leikooo.yupicturebackend.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.leikooo.yupicturebackend.dao.UserDAO;
import com.leikooo.yupicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.leikooo.yupicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.leikooo.yupicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.leikooo.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.leikooo.yupicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.model.vo.UserVO;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/1/22
 * @description
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    /**
     * 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /**
     * 保存所有连接的会话，key: pictureId, value: 用户会话集合
     */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    /**
     * 某张图片的编辑记录，key: pictureId, value: 编辑记录，用于某个用户中途加入编辑的时候看到最新的编辑记录
     */
    private final Map<Long, List<TextMessage>> pictureEditRecodes = new ConcurrentHashMap<>();

    @Resource
    private UserDAO userDAO;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.computeIfAbsent(pictureId, k -> ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        // 构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(UserVO.objToVo(user));
        TextMessage textMessage = getTextMessage(pictureEditResponseMessage);
        // 广播给同一张图片的用户
        broadcastToPicture(pictureId, textMessage);
        broadcastToOneUser(pictureId, pictureEditRecodes.get(pictureId), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }


    public void handleEnterEditMessage(PictureEditContext pictureEditContext) throws Exception {
        User user = pictureEditContext.getUser();
        Long pictureId = pictureEditContext.getPictureId();
        // 没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 设置当前用户为编辑用户
            pictureEditingUsers.put(pictureId, user.getId());
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(UserVO.objToVo(user));
            TextMessage textMessage = getTextMessage(pictureEditResponseMessage);
            broadcastToPicture(pictureId, textMessage);
        }
        if (!Objects.equals(pictureEditingUsers.get(pictureId), user.getId())) {
            handleEditErrorMessage(user, pictureId, pictureEditContext.getSession());
        }
    }


    private void handleEditErrorMessage(User user, Long pictureId, WebSocketSession sendSession) throws Exception {
        // 没有权限
        Long editUserId = pictureEditingUsers.get(pictureId);
        User editUser = userDAO.getById(editUserId);
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
        pictureEditResponseMessage.setMessage(String.format("操作失败 %s 正在操作", editUser.getUserName()));
        pictureEditResponseMessage.setUser(UserVO.objToVo(user));
        broadcastToOneUser(pictureId, Collections.singletonList(getTextMessage(pictureEditResponseMessage)), sendSession);
    }

    public void handleEditActionMessage(PictureEditContext pictureEditContext) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditContext.getRequestMessage();
        WebSocketSession session = pictureEditContext.getSession();
        User user = pictureEditContext.getUser();
        Long pictureId = pictureEditContext.getPictureId();
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) {
            return;
        }
        // 确认是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), actionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(UserVO.objToVo(user));
            TextMessage textMessage = getTextMessage(pictureEditResponseMessage);
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            broadcastToPicture(pictureId, textMessage, session);
            PictureEditResponseMessage savePictureEditResponseMessage = new PictureEditResponseMessage();
            savePictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            savePictureEditResponseMessage.setEditAction(editAction);
            // 获取当前key对应的List，如果没有则创建一个新的List
            List<TextMessage> list = pictureEditRecodes.computeIfAbsent(pictureId, k -> new ArrayList<>());
            // 将新的TextMessage添加到List中
            list.add(getTextMessage(savePictureEditResponseMessage));
        }
    }

    public void handleExitEditMessage(PictureEditContext pictureEditContext) {
        Long pictureId = null;
        try {
            User user = pictureEditContext.getUser();
            pictureId = pictureEditContext.getPictureId();
            Long editingUserId = pictureEditingUsers.get(pictureId);
            if (editingUserId != null && editingUserId.equals(user.getId())) {
                // 构造响应，发送退出编辑的消息通知
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
                String message = String.format("%s退出编辑图片", user.getUserName());
                pictureEditResponseMessage.setMessage(message);
                pictureEditResponseMessage.setUser(UserVO.objToVo(user));
                TextMessage textMessage = getTextMessage(pictureEditResponseMessage);
                broadcastToPicture(pictureId, textMessage);
                // 移除当前用户的编辑状态
                pictureEditingUsers.remove(pictureId);
            }
            pictureSessions.get(pictureId).remove(pictureEditContext.getSession());

        } catch (Exception e) {
            log.error("PictureEditHandler#handleExitEditMessage {}", ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        } finally {
            // 对应图片没人操作那么就删除编辑记录，防止内存泄露
            if (pictureSessions.get(pictureId).isEmpty()) {
                pictureEditRecodes.remove(pictureId);
            }
        }
    }


    private void broadcastToPicture(Long pictureId, TextMessage textMessage, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    private TextMessage getTextMessage(PictureEditResponseMessage pictureEditResponseMessage) throws JsonProcessingException {
        // 创建 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        // 支持 long 基本类型
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        // 序列化为 JSON 字符串
        String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
        return new TextMessage(message);
    }

    /**
     * 全部广播
     */
    private void broadcastToPicture(Long pictureId, TextMessage textMessage) throws Exception {
        broadcastToPicture(pictureId, textMessage, null);
    }

    private void broadcastToOneUser(Long pictureId, List<TextMessage> textMessages, WebSocketSession sendSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            if (Objects.isNull(textMessages) || textMessages.isEmpty()) {
                return;
            }
            // 创建 ObjectMapper
            textMessages.forEach(textMessage -> {
                for (WebSocketSession session : sessionSet) {
                    // 给指定用户发送信息
                    if (session.isOpen() && session.equals(sendSession)) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            log.error("PictureEditHandler#broadcastToOneUser {}", ExceptionUtils.getRootCauseMessage(e));
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }
    }

    @Data
    @AllArgsConstructor
    public static class PictureEditContext {
        private PictureEditRequestMessage requestMessage;
        private WebSocketSession session;
        private User user;
        private Long pictureId;
    }
}
