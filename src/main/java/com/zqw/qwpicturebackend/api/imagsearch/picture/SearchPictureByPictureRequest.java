package com.zqw.qwpicturebackend.api.imagsearch.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片id   因为我们是对自己上传的图片进行像是查询，故要告诉带查询的图片 数据库id
     */
    private Long pictureId;

    private static final long serialVersionUID = -7358995140334444597L;
}
