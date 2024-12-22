//package com.leikooo.yupicturebackend.model.constant;
//
//import com.leikooo.yupicturebackend.exception.ErrorCode;
//import com.leikooo.yupicturebackend.exception.ThrowUtils;
//import lombok.Getter;
//import org.apache.commons.lang3.StringUtils;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * @author <a href="https://github.com/lieeew">leikooo</a>
// * @date 2024/12/20
// * @description
// */
//@Getter
//public enum AllowFileTypeConstant {
//    WEBP("image/webp"),
//    JPEG("image/jpeg"),
//    PNG("image/png"),
//    PNG_("png"),
//    WEBP_("webp"),
//    JPG_("jpg"),
//    JPEG_("jpeg");
//
//    private final String value;
//
//    AllowFileTypeConstant(String value) {
//        this.value = value;
//    }
//
//    public static boolean isValidFileTypeForUpdate(String fileType) {
//        ThrowUtils.throwIf(StringUtils.isBlank(fileType), ErrorCode.PARAMS_ERROR);
//        for (AllowFileTypeConstant fileTypeConstant : AllowFileTypeConstant.values()) {
//            if (fileTypeConstant.getValue().equals(fileType)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public static void main(String[] args) {
//        String fileType = "image/webp";
//        List<Boolean> collect = Arrays.stream(AllowFileTypeConstant.values())
//                .map(type -> type.getValue().equals(fileType))
//                // ture 会留下 false 会过滤掉
//                .filter(bool -> bool)
//                .collect(Collectors.toList());
//        System.out.println(isValidFileTypeForUpdate("image/webp"));
//    }
//}
