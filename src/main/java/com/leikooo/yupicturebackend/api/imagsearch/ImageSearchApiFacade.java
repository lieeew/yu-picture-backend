package com.leikooo.yupicturebackend.api.imagsearch;


import com.leikooo.yupicturebackend.api.imagsearch.model.ImageSearchResult;
import com.leikooo.yupicturebackend.api.imagsearch.sub.GetImageFirstUrlApi;
import com.leikooo.yupicturebackend.api.imagsearch.sub.GetImageListApi;
import com.leikooo.yupicturebackend.api.imagsearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageList = searchImage("https://www.codefather.cn/logo.png");
        System.out.println("结果列表" + imageList);
    }
}