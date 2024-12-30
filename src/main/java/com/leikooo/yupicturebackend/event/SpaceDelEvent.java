package com.leikooo.yupicturebackend.event;

import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.model.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/30
 * @description
 */
@Getter
public class SpaceDelEvent extends ApplicationEvent {

    private final List<Picture> pictures;

    public SpaceDelEvent(Object source, List<Picture> pictures) {
        super(source);
        this.pictures = pictures;
    }
}
