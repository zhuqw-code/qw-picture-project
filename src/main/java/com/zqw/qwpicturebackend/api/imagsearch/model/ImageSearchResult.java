package com.zqw.qwpicturebackend.api.imagsearch.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 相似
 */
@Data
@Slf4j
public class ImageSearchResult {
    /**
     * 原始图片
     */
    private String thumbUrl;

    /**
     * 像是图片
     */
    private String objUrl;

}
