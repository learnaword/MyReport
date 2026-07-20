package com.myreport.util.word.common;

import com.alibaba.fastjson.JSONObject;
import com.spire.doc.Section;
import com.spire.doc.documents.HorizontalAlignment;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.fields.TextRange;
import com.myreport.framework.redis.RedisFileStateUtil;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
    /**
     * 获取数据集
     */
    public static void collectProcessInformation(JSONObject overallSetting, String text) {
        String strDownloaderKey = overallSetting.getString("strDownloaderKey");
        RedisFileStateUtil.fileUpdateProgress(strDownloaderKey, text, 1);
    }

    /**
     * 收集异常信息
     */
    public static void collectProcessInformation(JSONObject overallSetting, Exception e, String text) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        collectProcessInformation(overallSetting, text + "：" + sw.toString());
    }

    /**
     * 信息提示
     */
    public static void warning(Section section, String text) {
        Paragraph paragraph = section.addParagraph();
        paragraph.getFormat().setHorizontalAlignment(HorizontalAlignment.Center);
        TextRange textRange = paragraph.appendText(text);
        textRange.getCharacterFormat().setTextColor(Color.RED);
    }
}
