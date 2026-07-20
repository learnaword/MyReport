package com.myreport.util.word.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.util.word.WordContant;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xwpf.usermodel.XWPFChart;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CoordinateUtil {

    /**
     * 更新坐标
     */
    public static void updateChartCoordinate(XWPFChart chart, JSONObject temlateObject, JSONArray jsonArrData) {
        Boolean bCoordinate = temlateObject.containsKey("bCoordinate") ? temlateObject.getBooleanValue("bCoordinate") : false;
        if(!bCoordinate){
            return;
        }
        Integer id = temlateObject.containsKey("id") ? temlateObject.getInteger("id") : 0;
        //占比和人数图
        if(WordContant.COUNT_RATIO_CHART_TYPES.contains(id)){
            setCountRatioChartCoordinate(chart, jsonArrData);
        }
    }

    /**
     * 更新占比和人数图坐标
     */
    private static void setCountRatioChartCoordinate(XWPFChart chart, JSONArray jsonArrData) {
        double maximum = getMaxinum(jsonArrData);
        for (int i = 1; i < jsonArrData.size(); i++) {
            JSONArray item = jsonArrData.getJSONArray(i);
            if (item != null && item.size() > 1) {
                item.set(1, maximum);
            }
        }
        List<XDDFChartData> chartDatas = chart.getChartSeries();
        for (XDDFChartData chartData : chartDatas) {
            if (!chartData.getValueAxes().isEmpty()) {
                chartData.getValueAxes().get(0).setMaximum(maximum);
            }
        }
    }

    /**
     * 获取最大值
     */
    private static double getMaxinum(JSONArray jsonArrData){
        double maximum = 0.0;
        for (int i = 1; i < jsonArrData.size(); i++) {
            JSONArray item = jsonArrData.getJSONArray(i);
            String valueItem = item.getString(2);
            double doubleValue = Double.parseDouble(valueItem.replace("%", "")) / 100.0;
            if(doubleValue > maximum){
                maximum = doubleValue;
            }
            if (item != null && item.size() > 1) {
                item.set(1, maximum);
            }
        }
        //保留一位小数
        maximum = new BigDecimal(maximum * 1.3)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        return maximum;
    }
}
