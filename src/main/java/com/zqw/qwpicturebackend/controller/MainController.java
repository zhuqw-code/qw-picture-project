package com.zqw.qwpicturebackend.controller;

import com.zqw.qwpicturebackend.common.BaseResult;
import com.zqw.qwpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping("/health")
    public BaseResult<?> health() {
        return ResultUtils.success("ok");
    }
}
