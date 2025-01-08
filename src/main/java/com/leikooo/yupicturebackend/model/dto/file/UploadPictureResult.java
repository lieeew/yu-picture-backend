package com.leikooo.yupicturebackend.model.dto.file;

import com.leikooo.yupicturebackend.model.entity.Urls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadPictureResult {

    /**
     * url 集合
     */
    private Urls urls;

    /**  
     * 图片名称  
     */  
    private String picName;


    /**
     * 图片主色调
     */
    private String picColor;

    /**  
     * 文件体积  
     */  
    private Long picSize;  
  
    /**  
     * 图片宽度  
     */  
    private int picWidth;  
  
    /**  
     * 图片高度  
     */  
    private int picHeight;  
  
    /**  
     * 图片宽高比  
     */  
    private Double picScale;  
  
    /**  
     * 图片格式  
     */  
    private String picFormat;  
  
}
