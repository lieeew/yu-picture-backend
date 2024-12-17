package com.leikooo.yupicturebackend;

import com.leikooo.yupicturebackend.dao.PictureDAO;
import com.leikooo.yupicturebackend.manager.FileManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class YuPictureBackendApplicationTests {
    @Resource
    private PictureDAO pictureDAO;

    @Test
    void contextLoads() {
//        boolean b = pictureDAO.updateById();
    }

}
