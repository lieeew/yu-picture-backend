package com.leikooo.yupicturebackend.commen;

import lombok.Data;

import java.io.Serializable;

/**
 * @author liang
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
