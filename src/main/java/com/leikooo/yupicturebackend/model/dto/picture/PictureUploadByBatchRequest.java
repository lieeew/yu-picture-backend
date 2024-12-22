package com.leikooo.yupicturebackend.model.dto.picture;

import lombok.Data;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Data
public class PictureUploadByBatchRequest {  
  
    /**  
     * 搜索词  
     */  
    private String searchText;

    /**
     * 名称前缀
     */
    private String namePrefix;

    /**  
     * 抓取数量  
     */  
    private Integer count = 10;  
}