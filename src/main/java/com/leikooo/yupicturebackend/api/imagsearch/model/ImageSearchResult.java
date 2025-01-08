package com.leikooo.yupicturebackend.api.imagsearch.model;

import lombok.Data;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Data
public class ImageSearchResult {  
  
    /**  
     * 缩略图地址  
     */  
    private String thumbUrl;  
  
    /**  
     * 来源地址  
     */  
    private String fromUrl;  
}
