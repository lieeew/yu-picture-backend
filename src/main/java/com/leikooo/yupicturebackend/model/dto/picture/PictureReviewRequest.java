package com.leikooo.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Data
public class PictureReviewRequest implements Serializable {
  
    /**  
     * picture Id
     */  
    private Long id;  
  
    /**  
     * 状态：0-待审核, 1-通过, 2-拒绝
     * @see com.leikooo.yupicturebackend.model.enums.PictureReviewStatusEnum
     */  
    private Integer reviewStatus;  
  
    /**  
     * 审核信息  
     */  
    private String reviewMessage;  
  
  
    private static final long serialVersionUID = 1L;  
}
