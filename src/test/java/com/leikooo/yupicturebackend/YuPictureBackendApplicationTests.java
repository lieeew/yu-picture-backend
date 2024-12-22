package com.leikooo.yupicturebackend;

import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.leikooo.yupicturebackend.manager.upload.UrlPictureUpload;
import com.leikooo.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.leikooo.yupicturebackend.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class YuPictureBackendApplicationTests {
    @Resource
    private PictureDAO pictureDAO;

//    @Resource
//    private UrlPictureUpload urlPictureUpload;


    @Test
    void contextLoads() {
        User user = new User();
        user.setId(1867564572769492994L);
        // urlPictureUpload.uploadPicture("https://leeikoooo-1313589692.cos.ap-beijing.myqcloud.com/public/1867564572769492994/2024-12-21_qxHVSSqsCedy8xqb.编程真是一辈都学不完.png", new PictureUploadRequest(), user);
    }

}
