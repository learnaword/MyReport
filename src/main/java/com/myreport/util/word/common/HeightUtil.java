package com.myreport.util.word.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.myreport.util.word.WordContant;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xddf.usermodel.chart.XDDFChartAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xwpf.usermodel.XWPFChart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class HeightUtil {

    /**
     * 设置图表高度
     */
    public static void updateChartHeight(XWPFChart chart, JSONObject temlateObject, JSONArray jsonArrData, int dataSize) {
        Boolean updateH = temlateObject.containsKey("updateH") ? temlateObject.getBooleanValue("updateH") : false;
        if(!updateH){
            return;
        }

        Integer chartType = temlateObject.getInteger("type");
        Integer id = temlateObject.containsKey("id") ? temlateObject.getInteger("id") : 0;
        if (chartType == WordContant.chartType.LINE_AND_BAR_CHART) {
            updateLineAndBarChartHeight(chart, id, jsonArrData);
            return;
        }

        if (chartType == WordContant.chartType.BAR_X_CHART) {
            setBarxHeight(chart, temlateObject, jsonArrData, dataSize);
            setBarxMaximum(chart, temlateObject, jsonArrData);
            return;
        }
    }

    /**
     * 设置图表高度通过id
     */
    private static void updateLineAndBarChartHeight(XWPFChart chart, Integer id, JSONArray jsonArrData) {
        XWPFDocument charParent = (XWPFDocument) chart.getParent();
        XWPFParagraph paragraph = charParent.getLastParagraph();
        paragraph.getCTP();
        XWPFRun run = paragraph.getRuns().get(0);
        CTDrawing drawing = run.getCTR().getDrawingArray(0);
        CTInline ctInline = drawing.getInlineArray(0);
        Long defaultSize = 472L;
        Long height = defaultSize + (getChartHeight(jsonArrData, id) * 116);
        ctInline.getExtent().setCy(height * 3600);
    }

    /**
     * 设置条形图高度
     */
    private static void setBarxHeight(XWPFChart chart, JSONObject temlateObject, JSONArray jsonArrData, int dataSize) {
        if (!temlateObject.containsKey("height")) {
            return;
        }
        int height = temlateObject.getInteger("height");
        String chartName = temlateObject.getString("name");
        XWPFDocument charParent = (XWPFDocument) chart.getParent();
        XWPFParagraph paragraph = charParent.getLastParagraph();
        paragraph.getCTP();
        XWPFRun run = paragraph.getRuns().get(0);
        CTDrawing drawing = run.getCTR().getDrawingArray(0);
        CTInline ctInline = drawing.getInlineArray(0);
        if (chartName.equals("条形图")) {
            boolean isPercentum = isPercentum(jsonArrData);
            BarXData barXData = getBarXData(jsonArrData, isPercentum);
            List<String> xData = barXData.getxData();
            int maxLen = xData.stream().filter(s -> s != null).mapToInt(String::length).max().orElse(0);
            if (maxLen < 16) {
                ctInline.getExtent().setCy((dataSize - 1) * 70 * 3600);
            } else {
                ctInline.getExtent().setCy((dataSize - 1) * 90 * 3600);
            }
        } else {
            ctInline.getExtent().setCy((dataSize - 1) * height * 3600);
        }
    }


    private static void setBarxMaximum(XWPFChart chart, JSONObject temlateObject, JSONArray jsonArrData) {
        String chartName = temlateObject.getString("name");
        if (chartName.equals("条形图")) {
            boolean isPercentum = isPercentum(jsonArrData);
            BarXData barXData = getBarXData(jsonArrData, isPercentum);
            List<Double> yData = barXData.getyData();
            double maxValue = yData.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).filter(d -> !Double.isNaN(d)).max().orElse(1.0);
            List<? extends XDDFChartAxis> chartAxes = chart.getAxes();
            XDDFChartAxis chartAxis = chartAxes.stream().filter(item -> item instanceof XDDFValueAxis).collect(Collectors.toList()).get(0);
            if (isPercentum) {
                chartAxis.setMaximum(maxValue / 100 * 1.3);
            } else {
                chartAxis.setMaximum(maxValue * 1.3);
            }
        }
    }

    /**
     * 获取设置图表高度
     */
    public static Integer getChartHeight(JSONArray dataObject, Integer chartId) {
        if (chartId == 68) {
            List<Double> colSizeList = Arrays.asList(31D, 15D, 10D, 7D, 6D, 5D, 4D, 3D, 3D, 3D, 2D, 2D, 2D, 2D, 2D, 2D, 2D, 2D, 2D, 2D);
            Integer rowSize = dataObject.size() - 1;
            Set<String> currentSet = new HashSet<>();
            for (int j = 1; j < dataObject.size(); j++) {
                String strRowName = dataObject.getJSONArray(j).getString(0);
                currentSet.add(strRowName);
            }
            int maxLength = currentSet.stream()
                    .mapToInt(HeightUtil::estimateLength)
                    .max()
                    .orElse(0);
            Double colSize = colSizeList.get(rowSize - 1);
            int rowSizeItem = (int) Math.ceil(maxLength / colSize);
            return rowSizeItem;
        }
        return 0;
    }

    /**
     * 粗略估算字符串长度
     */
    public static int estimateLength(String text) {
        double length = text.chars()
                .mapToDouble(c -> isChinese((char) c) ? 1.0 : 0.5)
                .sum();
        return (int) Math.ceil(length);
    }

    /**
     * 判断是否为中文字符
     */
    public static boolean isChinese(char c) {
        return String.valueOf(c).matches("[\u4e00-\u9fa5]");
    }

    /**
     * 获取横向坐标轴数据
     *
     * @param barDatas 标题
     */
    private static BarXData getBarXData(JSONArray barDatas, boolean isPercentum) {
        if (barDatas == null) {
            throw new IllegalArgumentException("chartJson 不能为空");
        }

        if (barDatas == null || barDatas.isEmpty()) {
            throw new IllegalArgumentException("barDatas 不能为空");
        }

        // 获取数据
        List<String> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        for (int i = 1; i < barDatas.size(); i++) {
            JSONArray item = barDatas.getJSONArray(i);
            xData.add(item.getString(0));
            if (isPercentum) {
                Object valueItem = item.get(1);
                yData.add(getBarXDoubleValue(valueItem));
            } else {
                yData.add(item.getDouble(1));
            }
        }
        return new BarXData(xData, yData);
    }

    private static Double getBarXDoubleValue(Object valueItem) {
        if (valueItem instanceof Double) {
            return (Double) valueItem * 100;
        } else if (valueItem instanceof Integer) {
            return ((Integer) valueItem).doubleValue() * 100;
        } else if (valueItem instanceof Long) {
            return ((Long) valueItem).doubleValue() * 100;
        } else if (valueItem instanceof Float) {
            return ((Float) valueItem).doubleValue() * 100;
        } else if (valueItem instanceof BigDecimal) {
            return ((BigDecimal) valueItem).doubleValue() * 100;
        } else if (valueItem instanceof String) {
            String valueStr = (String) valueItem;
            if (valueStr.contains("%")) {
                valueStr = valueStr.replace("%", "").trim();
                return Double.parseDouble(valueStr);
            } else {
                return Double.parseDouble(valueStr) * 100;
            }
        } else {
            throw new IllegalArgumentException("valueItem 必须是数字类型 (Double, Integer, Long, Float, BigDecimal) 或 String 类型: " + valueItem);
        }
    }

    /**
     * 判断是否需要转换为百分比
     *
     * @param jsonData 数据
     */
    private static boolean isPercentum(JSONArray jsonData) {
        String valColText = jsonData.getJSONArray(1).getString(1);
        if (StringUtils.isNotEmpty(valColText) && valColText.contains("%")) {
            return true;
        }
        return false;
    }

    /**
     * 条形图横纵轴数据。
     */
    private static class BarXData {
        private final List<String> xData;
        private final List<Double> yData;

        BarXData(List<String> xData, List<Double> yData) {
            this.xData = xData;
            this.yData = yData;
        }

        public List<String> getxData() {
            return xData;
        }

        public List<Double> getyData() {
            return yData;
        }
    }
}
