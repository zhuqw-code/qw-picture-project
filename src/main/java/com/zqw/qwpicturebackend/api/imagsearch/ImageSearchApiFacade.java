package com.zqw.qwpicturebackend.api.imagsearch;

import com.zqw.qwpicturebackend.api.imagsearch.model.ImageSearchResult;
import com.zqw.qwpicturebackend.api.imagsearch.sub.GetImageFirstUrlApi;
import com.zqw.qwpicturebackend.api.imagsearch.sub.GetImageListApi;
import com.zqw.qwpicturebackend.api.imagsearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

    public static void main(String[] args) {
        // List<ImageSearchResult> imageList = searchImage("https://shitu-query-bj.bj.bcebos.com/2025-11-17/22/b588a09912d7e899?authorization=bce-auth-v1%2F7e22d8caf5af46cc9310f1e3021709f3%2F2025-11-17T14%3A33%3A39Z%2F300%2Fhost%2Fbb155defaf5a3a5be355dc465ce5cc629970e0ba79b685d32c26c65cb8e3f920");
        List<ImageSearchResult> imageList = searchImage("https://shitu-query-bj.bj.bcebos.com/2025-11-17/22/ef40bd00064c62e8?authorization=bce-auth-v1%2F7e22d8caf5af46cc9310f1e3021709f3%2F2025-11-17T14%3A34%3A28Z%2F300%2Fhost%2Fea58f6ffe5a5f95d209f06788981889b9e6cb6e0d80dd44010c564b08140cf64");
        System.out.println("结果列表" + imageList);
    }
}
