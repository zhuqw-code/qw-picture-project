package com.zqw.qwpicturebackend.model.dto.file;

import lombok.Data;

@Data
public class UploadPictureResult {

    /**
     * 上传到COS的图片路径
     */
    private String url;
    /**
     * 压缩图片 thumbnailUrl
     */
    private String thumbnailUrl;
    /**
     * 图片名称
     */
    private String picName;
    /**
     * 文件大小
     */
    private Long picSize;
    /**
     * 图片宽度
     */
    private Integer width;
    /**
     * 图片高度
     */
    private Integer height;
    /**
     * 图片纵横比
     */
    private Double picScale;
    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;
}
