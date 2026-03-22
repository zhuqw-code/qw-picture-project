package com.zqw.qwpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * id(因为用户上传图片后无论有最后有没有添加，我们都将其存储起来)
     */
    private Long id;

    /**
     * 通过url上传
     */
    private String fileUrl;

    /**
     * 批量抓取的图片要设置名称，调用上传图片方法，只能通过 PictureUploadRequest 进行传递
     */
    private String picName;

    /**
     * 设置上传图片的空间id
     */
    private Long spaceId;

    private static final long serialVersionUID = 6855849219570367799L;
}
