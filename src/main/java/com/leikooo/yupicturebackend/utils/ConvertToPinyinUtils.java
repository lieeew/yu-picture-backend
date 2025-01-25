package com.leikooo.yupicturebackend.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/1/24
 * @description
 */
public class ConvertToPinyinUtils {

    /**
     * 辅助方法：将包含中文的字符串转换为拼音
     * @param input
     * @return
     */
    public static String convertToPinyinIfChinese(String input) {
        StringBuilder result = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);

        for (char c : input.toCharArray()) {
            try {
                // 如果是中文字符，转换为拼音；否则直接添加
                if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null) {
                        // 取首个拼音
                        result.append(pinyinArray[0]);
                    }
                } else {
                    result.append(c);
                }
            } catch (Exception e) {
                // 如果转换失败，保留原字符
                result.append(c);
            }
        }

        return result.toString();
    }

}
