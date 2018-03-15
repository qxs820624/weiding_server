package zs.live.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Created by Administrator on 2016/1/12.
 */
@Component
class ImageUtils {

    /**
     * 将又拍云后缀变为50*50大小的图片
     * @param imagePath
     * @return
     */
    static String getSmallImg(String imagePath){
        if(!imagePath){
            return imagePath
        }
        //又拍云格式图片，如果没有!后缀，则需要补加上,阿里云后缀与U盘云格式一致
        if (imagePath.indexOf("upaiyun.com") > -1 && (imagePath.indexOf("souyue-xqq") > -1
            || imagePath.indexOf("user-pimg") > -1 || imagePath.indexOf("souyue-image") >-1)
            || imagePath.indexOf("aliyuncs") > -1) {
            if (imagePath.contains("!")) {
                int index = imagePath.indexOf("!");
                if (index > 0) {
                    imagePath = imagePath.substring(0, index);
                }
            }
            imagePath = imagePath + "!userhead50x50"
        }
        return imagePath
    }

    /**
     * 直播列表正方形图片使用
     * @param imagePath
     * @return
     */
    static String getLiveListImg(String imagePath){
        if(!imagePath){
            return imagePath
        }
        //又拍云格式图片，如果没有!后缀，则需要补加上,阿里云后缀与U盘云格式一致
        if (imagePath.indexOf("upaiyun.com") > -1 && (imagePath.indexOf("souyue-xqq") > -1
            || imagePath.indexOf("user-pimg") > -1 || imagePath.indexOf("souyue-image") >-1)
            || imagePath.indexOf("aliyuncs") > -1) {
            if (imagePath.contains("!")) {
                int index = imagePath.indexOf("!");
                if (index > 0) {
                    imagePath = imagePath.substring(0, index);
                }
            }
            imagePath = imagePath + "!zb"
        }
        return imagePath
    }

    /**
     * 搜索直播列表正方形图片使用
     * @param imagePath
     * @return
     */
    static String getSearchLiveListImg(String imagePath){
        if(!imagePath){
            return imagePath
        }
        //又拍云格式图片，如果没有!后缀，则需要补加上,阿里云后缀与U盘云格式一致
        if (imagePath.indexOf("upaiyun.com") > -1 && (imagePath.indexOf("souyue-xqq") > -1
            || imagePath.indexOf("user-pimg") > -1 || imagePath.indexOf("souyue-image") >-1 || imagePath.indexOf("sns-img") > -1)
            || imagePath.indexOf("aliyuncs") > -1) {
            if (imagePath.contains("!")) {
                int index = imagePath.indexOf("!");
                if (index > 0) {
                    imagePath = imagePath.substring(0, index);
                }
            }
            imagePath = imagePath + "!ltj"
        }
        return imagePath
    }
}
