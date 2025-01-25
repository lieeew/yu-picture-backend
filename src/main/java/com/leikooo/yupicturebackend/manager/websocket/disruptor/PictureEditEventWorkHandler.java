package com.leikooo.yupicturebackend.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.leikooo.yupicturebackend.manager.websocket.PictureEditHandler;
import com.leikooo.yupicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.leikooo.yupicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.leikooo.yupicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.leikooo.yupicturebackend.model.entity.User;
import com.leikooo.yupicturebackend.service.UserService;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent event) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();
        // 获取到消息类别
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.valueOf(type);
        PictureEditHandler.PictureEditContext pictureEditContext = new PictureEditHandler.PictureEditContext(pictureEditRequestMessage, session, user, pictureId);
        // 调用对应的消息处理方法
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditContext);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditContext);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditContext);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(pictureEditResponseMessage)));
        }
    }
}