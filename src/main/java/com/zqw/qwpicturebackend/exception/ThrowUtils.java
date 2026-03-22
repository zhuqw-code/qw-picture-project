package com.zqw.qwpicturebackend.exception;

/**
 * 由于重复的if判断，进而抛出异常，我们直接在这里优化为一行代码
 */
public class ThrowUtils {

    /**
     * 判断是否抛出异常
     *
     * @param condition 判断信息，为true就抛出信息
     * @param exception 所有的异常均可
     */
    public static void throwif(boolean condition, RuntimeException exception) {
        if (condition) {
            throw exception;
        }
    }

    /**
     * 判断是否抛出异常
     *
     * @param condition 判断信息，为true就抛出信息
     * @param errorCode 错误信息枚举类型
     * @param message   使用具体的错误信息
     */
    public static void throwif(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 判断是否抛出异常
     *
     * @param condition 判断信息，为true就抛出信息
     * @param errorCode 错误信息枚举类型
     */
    public static void throwif(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 判断是否抛出异常
     *
     * @param condition 判断信息，为true就抛出信息
     * @param code      错误码
     * @param message   错误信息
     */
    public static void throwif(boolean condition, int code, String message) {
        if (condition) {
            throw new BusinessException(code, message);
        }
    }
}
