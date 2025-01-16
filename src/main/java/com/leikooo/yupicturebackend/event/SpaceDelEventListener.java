package com.leikooo.yupicturebackend.event;

import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import com.leikooo.yupicturebackend.exception.ThrowUtils;
import com.leikooo.yupicturebackend.model.entity.Picture;
import com.leikooo.yupicturebackend.service.PictureService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/30
 * @description
 */
@Component
@AllArgsConstructor
public class SpaceDelEventListener {

    private final PictureService pictureService;

    private final PictureDAO pictureDAO;

    @TransactionalEventListener(value = SpaceDelEvent.class, phase = TransactionPhase.BEFORE_COMMIT)
    public void onSpaceDelEvent(SpaceDelEvent event) {
        String currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        TransactionSynchronizationManager.isActualTransactionActive();
        List<Picture> pictures = event.getPictures();
        ThrowUtils.throwIf(pictures == null || pictures.isEmpty(), ErrorCode.SYSTEM_ERROR, "删除失败");
        pictureDAO.deleteBatchIds(pictures.stream().map(Picture::getId).toList());
        pictures.forEach(pictureService::clearPictureFile);
    }
}
