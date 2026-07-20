package com.myreport.util.word;

import java.util.*;

public class WordContant {
    public static final int IMAGE_FULL_WIDTH = 435;
    // 多列条形图集合
    public static final List<Integer> MULTI_BAR_CHART_TYPES = Collections.unmodifiableList(Arrays.asList(53, 71, 73));
    //多列条形图百分比集合
    public static final List<Integer> MULTI_BAR_PERCENT_CHART_TYPES = Collections.unmodifiableList(Arrays.asList(54, 72));
    // 三维气泡图集合
    public static final List<Integer> BUBBLE_3D_CHART_TYPES = Collections.unmodifiableList(Arrays.asList(34, 35, 63, 97, 98, 106));
    // 占比和人数图
    public static final List<Integer> COUNT_RATIO_CHART_TYPES = Collections.unmodifiableList(Arrays.asList(88, 94));
    public class chartType {
        //横向柱状图
        public static final int BAR_X_CHART = 3;
        //静态图
        public static final int STATIC_CHART = 10;
        //折线柱状图-单柱
        public static final int LINE_AND_BAR_CHART = 13;
        //组合图
        public static final int COMBINATION_CHART = 11;
        //散点图
        public static final int SANDIAN_CHART = 12;
        //echarts图
        public static final int ECHARTS_CHART = 99;
    }
    //各种报告模板
    public class template {
        //spireReport模板
        public static final String SPIRE_REPORT_TEMPLATE = "word/template/template.docx";
        //spireWordReport模板
        public static final String SPIRE_WORD_REPORT_TEMPLATE = "word/template/wordTemplate.docx";
        //spireOrgReport模板
        public static final String SPIRE_ORG_REPORT_TEMPLATE = "word/template/template.docx";
        //wordUtil模板
        public static final String SPIRE_WORD_UTIL_TEMPLATE = "word/template/tableTemplate.docx";
    }
    public class seriesType {
        //折线图
        public static final int LINE_SERIES = 1;
        //饼图
        public static final int PIE_SERIES = 2;
        //柱状图
        public static final int BAR_SERIES = 3;
        //散点图
        public static final int BAR_3D_SERIES = 4;
    }

    public static final Map<Integer, String> relationshipMap = new HashMap<Integer, String>() {{
        put(1, "{\"name\":\"对比折线图\",\"type\":1,\"template\":\"1.docx\",\"config\":[[{\"type\":1,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":1,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(2, "{\"name\":\"平滑折线图\",\"type\":1,\"template\":\"2.docx\",\"config\":[[{\"type\":1,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(3, "{\"name\":\"折线图\",\"type\":1,\"template\":\"3.docx\",\"config\":[[{\"type\":1,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(4, "{\"name\":\"散点图\",\"type\":12,\"template\":\"4.docx\",\"config\":[]}");
        put(5, "{\"name\":\"不同颜色条形图\",\"type\":3, \"updateH\":true, \"height\":65,\"template\":\"5.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(6, "{\"name\":\"反向条形图\",\"type\":3, \"updateH\":true, \"height\":65,\"template\":\"6.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(7, "{\"name\":\"堆积条形图\",\"type\":3,\"template\":\"7.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":4,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":5,\"extCol\":-1}]]}");
        put(8, "{\"name\":\"堆积条形图2\",\"type\":3,\"updateH\":true, \"height\":65,\"template\":\"8.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(9, "{\"name\":\"多行堆积条形图\",\"type\":3,\"updateH\":true, \"height\":65,\"template\":\"9.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":4,\"extCol\":-1}]]}");
        put(10, "{\"name\":\"对称对比条形图\",\"type\":3,\"updateH\":true, \"height\":90,\"template\":\"10.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":4,\"extCol\":6}]]}");
        put(11, "{\"name\":\"条形图\",\"type\":3, \"updateH\":true, \"height\":90,\"template\":\"11.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(12, "{\"name\":\"条形对比图\",\"type\":3,\"updateH\":true, \"height\":30,\"template\":\"12.docx\",\"config\":[]}");
        put(13, "{\"name\":\"辅助条形图\",\"type\":3, \"updateH\":true, \"height\":109,\"template\":\"13.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(14, "{\"name\":\"图案柱状图1\",\"type\":4,\"template\":\"14.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(15, "{\"name\":\"图案柱状图2\",\"type\":4,\"template\":\"15.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(16, "{\"name\":\"图案柱状图3\",\"type\":4,\"template\":\"16.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(17, "{\"name\":\"堆积柱状图\",\"type\":4,\"template\":\"17.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1}]]}");
        put(18, "{\"name\":\"多柱柱状图1\",\"type\":4,\"template\":\"18.docx\",\"config\":[]}");
        put(19, "{\"name\":\"多柱柱状图2\",\"type\":4,\"template\":\"19.docx\",\"config\":[]}");
        put(20, "{\"name\":\"带阴影柱状图\",\"type\":4,\"template\":\"20.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(21, "{\"name\":\"柱形对比图1\",\"type\":4,\"template\":\"21.docx\",\"config\":[]}");
        put(22, "{\"name\":\"柱形对比图2\",\"type\":4,\"template\":\"22.docx\",\"config\":[]}");
        put(23, "{\"name\":\"柱状图\",\"type\":4,\"template\":\"23.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(24, "{\"name\":\"条形图-条形图组合图\",\"type\":3, \"updateH\":true,\"height\":51,\"template\":\"24.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":1}],[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(25, "{\"name\":\"柱形图-折线图组合图1\",\"type\":5,\"template\":\"25.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}],[{\"type\":1,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(26, "{\"name\":\"柱形图-折线图组合对比图\",\"type\":5,\"template\":\"26.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1}],[{\"type\":1,\"updateTx\":false,\"category\":0,\"values\":3,\"extCol\":-1}]]}");
        put(27, "{\"name\":\"柱形图-折线图组合对比图2\",\"type\":5,\"template\":\"27.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}],[{\"type\":1,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(28, "{\"name\":\"饼图-圆环图组合图\",\"type\":5,\"template\":\"28.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}],[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(29, "{\"name\":\"饼图-雷达图组合图\",\"type\":5,\"template\":\"29.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1}],[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(30, "{\"name\":\"环形雷达图\",\"type\":6,\"template\":\"30.docx\",\"config\":[[{\"type\":2,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(31, "{\"name\":\"蛛网雷达图\",\"type\":6,\"template\":\"31.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(32, "{\"name\":\"面积图\",\"type\":7,\"template\":\"32.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(33, "{\"name\":\"圆环图\",\"type\":8,\"template\":\"33.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(34, "{\"name\":\"复合条饼图\",\"size\":7,\"isRowDynamic\":true,\"type\":8,\"template\":\"34.docx\",\"config\":[]}");
        put(35, "{\"name\":\"子母饼图\",\"size\":4,\"type\":8,\"template\":\"35.docx\",\"config\":[]}");
        put(36, "{\"name\":\"异形圆环图\",\"type\":8,\"template\":\"36.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(37, "{\"name\":\"饼图\",\"type\":8,\"template\":\"37.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(38, "{\"name\":\"辅助线饼图\",\"type\":8,\"template\":\"38.docx\",\"config\":[[{\"type\":2,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":3}]]}");
        put(39, "{\"name\":\"男女比例图\",\"type\":9,\"template\":\"39.docx\",\"config\":[[{\"type\":4,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":3}]]}");
        put(40, "{\"name\":\"高职广东省生源地区分布\",\"type\":10,\"template\":\"40.docx\",\"config\":[]}");
        put(41, "{\"name\":\"高职广东省就业地区分布\",\"type\":10,\"template\":\"41.docx\",\"config\":[]}");
        put(42, "{\"name\":\"均值条形图\",\"updateH\":true, \"height\":63,\"type\":3,\"template\":\"42.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":1}],[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(43, "{\"name\":\"性别柱形图\",\"type\":5,\"template\":\"43.docx\",\"config\":[[{\"type\":4,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":4,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(44, "{\"name\":\"行业柱形图\",\"type\":5,\"template\":\"44.docx\",\"config\":[[{\"type\":4,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(45, "{\"name\":\"毕业去向图\",\"type\":10,\"template\":\"45.docx\",\"config\":[]}");
        put(46, "{\"name\":\"多柱柱形图\",\"type\":4,\"isColDynamic\":true,\"template\":\"46.docx\",\"config\":[]}");
        put(47, "{\"name\":\"择业竞争力\",\"type\":10,\"template\":\"47.docx\",\"config\":[]}");
        put(48, "{\"name\":\"多柱柱形图3\",\"type\":4,\"template\":\"48.docx\",\"config\":[]}");
        put(49, "{\"name\":\"5维雷达图\",\"type\":4,\"template\":\"49.docx\",\"config\":[[{\"type\":2,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(50, "{\"name\":\"2025性别图\",\"type\":10,\"template\":\"50.docx\",\"config\":[]}");
        put(51, "{\"name\":\"2025毕业去向图\",\"type\":10,\"template\":\"51.docx\",\"config\":[]}");
        put(52, "{\"name\":\"重点区域图\",\"type\":10,\"template\":\"52.docx\",\"config\":[]}");
        put(53, "{\"name\":\"多列条形图\",\"type\":3,\"updateH\":true, \"height\":120, \"template\":\"53.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}, {\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":4,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":5,\"extCol\":-1}]]}");
        put(54, "{\"name\":\"多列条形图（百分比）\",\"type\":3,\"updateH\":true, \"height\":120, \"template\":\"54.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}, {\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":4,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":5,\"extCol\":-1}]]}");
        put(55, "{\"name\":\"带图标饼图\",\"type\":8,\"template\":\"55.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(56, "{\"name\":\"求职投入与收获\",\"type\":10,\"template\":\"56.docx\",\"config\":[]}");
        put(57, "{\"name\":\"用人单位对毕业生实际工作中发挥的作用与未来发展潜力评价\",\"type\":10,\"template\":\"57.docx\",\"config\":[]}");
        put(58, "{\"name\":\"用人单位对毕业生政治思想与职业素养水平评价\",\"type\":10,\"template\":\"58.docx\",\"config\":[]}");
        put(59, "{\"name\":\"用人单位对毕业生专业水平评价\",\"type\":10,\"template\":\"59.docx\",\"config\":[]}");
        put(60, "{\"name\":\"支持资源与管理服务的满意度评价\",\"type\":10,\"template\":\"60.docx\",\"config\":[]}");
        put(61, "{\"name\":\"紧凑柱状图\",\"type\":4,\"template\":\"61.docx\",\"config\":[]}");
        put(62, "{\"name\":\"柱状图-3列\",\"type\":4,\"template\":\"62.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(63, "{\"name\":\"子母饼图-就业稳定性\",\"size\":4, \"isRowDynamic\":true, \"type\":8,\"template\":\"63.docx\",\"config\":[]}");
        put(64, "{\"name\":\"堆叠条形图-求职有效性\",\"type\":3,\"template\":\"64.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":4,\"extCol\":-1}]]}");
        put(65, "{\"name\":\"雷达图-5项百分比\",\"type\":6,\"template\":\"65.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(66, "{\"name\":\"柱形图-三类四柱\",\"type\":4,\"template\":\"66.docx\",\"config\":[]}");
        put(67, "{\"name\":\"饼图-导师满意度\",\"type\":8,\"template\":\"67.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(68, "{\"name\":\"折线柱状图-单柱\",\"id\":68,\"updateH\":true,\"type\":13,\"template\":\"68.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}],[{\"type\":1,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(69, "{\"name\":\"柱状图-5柱\",\"type\":4,\"template\":\"69.docx\",\"config\":[]}");
        put(70, "{\"name\":\"柱状图-3柱\",\"type\":4,\"template\":\"70.docx\",\"config\":[]}");
        put(71, "{\"name\":\"多列条形图-6项\",\"type\":3,\"template\":\"71.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}, {\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":4,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":5,\"extCol\":-1}]]}");
        put(72, "{\"name\":\"多列条形图（百分比）-8项\",\"type\":3,\"template\":\"72.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}, {\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":4,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":5,\"extCol\":-1}]]}");
        put(73, "{\"name\":\"多列条形图-18项\",\"type\":3,\"template\":\"73.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}, {\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":4,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":5,\"extCol\":-1}]]}");
        put(74, "{\"name\":\"紧凑柱状图-4柱\",\"type\":4,\"isColDynamic\":true,\"template\":\"74.docx\",\"config\":[]}");
        put(75, "{\"name\":\"柱状图-5柱2行\",\"type\":4,\"template\":\"75.docx\",\"config\":[]}");
        put(76, "{\"name\":\"柱状图-分生源就业率\",\"type\":4,\"template\":\"76.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(77, "{\"name\":\"无坐标轴柱形图\",\"type\":4,\"template\":\"77.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(78, "{\"name\":\"饼图-饼图\",\"type\":11,\"template\":\"78.docx\",\"config\":[[[{\"type\":3,\"fillDataType\":\"normal\",\"updateTx\":false,\"category\":0,\"categoryIndex\":0,\"values\":1,\"valueIndex\":1,\"extCol\":-1}]],[[{\"type\":3,\"fillDataType\":\"normal\",\"updateTx\":false,\"category\":0,\"values\":1,\"categoryIndex\":2,\"valueIndex\":3,\"extCol\":-1}]]]}");
        put(79, "{\"name\":\"饼图-柱状图\",\"type\":11,\"template\":\"79.docx\",\"config\":[[[{\"type\":3,\"fillDataType\":\"normal\",\"updateTx\":false,\"category\":0,\"categoryIndex\":0,\"values\":1,\"valueIndex\":1,\"extCol\":-1}]],[[{\"type\":3,\"fillDataType\":\"normal\",\"updateTx\":false,\"category\":0,\"values\":1,\"categoryIndex\":2,\"valueIndex\":3,\"extCol\":-1}]]]}");
        put(80, "{\"name\":\"高职单位专业水平评价\",\"type\":10,\"template\":\"80.docx\",\"config\":[]}");
        put(81, "{\"name\":\"母校推荐度饼图\",\"type\":8,\"template\":\"81.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(82, "{\"name\":\"能力四项\",\"type\":4,\"template\":\"82.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(83, "{\"name\":\"高职就业现状满意度\",\"type\":10,\"template\":\"83.docx\",\"config\":[]}");
        put(84, "{\"name\":\"高职求职难易程度\",\"type\":10,\"template\":\"84.docx\",\"config\":[]}");
        put(85, "{\"name\":\"高职择业竞争力\",\"type\":10,\"template\":\"85.docx\",\"config\":[]}");
        put(86, "{\"name\":\"图案柱形图\",\"type\":4,\"template\":\"86.docx\",\"config\":[[{\"type\":1,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":1,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(87, "{\"name\":\"组合-蓝底条形图\",\"type\":3,\"template\":\"87.docx\",\"updateH\":true, \"height\":50,\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1}],[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":1}]]}");
        put(88, "{\"name\":\"占比和人数图\",\"id\":88,\"type\":3,\"template\":\"88.docx\",\"updateH\":true,\"bCoordinate\":true, \"height\":71,\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":3},{\"type\":3,\"updateTx\":false,\"category\":0,\"valueType\":\"string\",\"values\":3,\"extCol\":-1}]]}");
        put(89, "{\"name\":\"单学历性别图\",\"type\":10,\"template\":\"89.docx\",\"config\":[]}");
        put(90, "{\"name\":\"双类别双柱图\",\"id\":90,\"updateH\":true,\"type\":8,\"template\":\"90.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}],[{\"type\":1,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(91, "{\"name\":\"紧凑柱状图-5柱\",\"type\":4,\"template\":\"91.docx\",\"config\":[]}");
        put(92, "{\"name\":\"国内升学表\",\"type\":10,\"template\":\"92.docx\",\"config\":[]}");
        put(93, "{\"name\":\"广东重点区域图\",\"type\":10,\"template\":\"93.docx\",\"config\":[]}");
        put(94, "{\"name\":\"占比和人数图\",\"id\":94,\"type\":3,\"template\":\"94.docx\",\"updateH\":false, \"bCoordinate\":true,\"height\":71,\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":2,\"extCol\":3},{\"type\":3,\"updateTx\":false,\"category\":0,\"valueType\":\"string\",\"values\":3,\"extCol\":-1}]]}");
        put(95, "{\"name\":\"广东生源图\",\"type\":10,\"template\":\"95.docx\",\"config\":[]}");
        put(96, "{\"name\":\"母校支持资源与管理服务图\",\"type\":10,\"template\":\"96.docx\",\"config\":[]}");
        put(97, "{\"name\":\"子母饼图-晋升率\",\"size\":4, \"type\":8, \"isRowDynamic\":true, \"template\":\"97.docx\",\"config\":[]}");
        put(98, "{\"name\":\"子母饼图-行业转换率\",\"size\":4, \"type\":8, \"isRowDynamic\":true, \"template\":\"98.docx\",\"config\":[]}");
        put(99, "{\"name\":\"柱状图-母校社会声誉\",\"type\":4,\"template\":\"99.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(100, "{\"name\":\"柱状图-中长期样式图\",\"type\":4,\"template\":\"100.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(101, "{\"name\":\"柱状图-均值\",\"type\":4,\"template\":\"101.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(102, "{\"name\":\"饼图-职称图\",\"type\":8,\"template\":\"102.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(103, "{\"name\":\"折线图-月均收入\",\"type\":1,\"template\":\"103.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(104, "{\"name\":\"其他-管理服务与资源条件图\",\"type\":10,\"template\":\"104.docx\",\"config\":[]}");
        put(105, "{\"name\":\"柱状图-受益度评价\",\"type\":4,\"template\":\"105.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(106, "{\"name\":\"子母饼图-职业转换率\",\"size\":4, \"type\":8,\"isRowDynamic\":true,\"template\":\"106.docx\",\"config\":[]}");
        put(107, "{\"name\":\"本科-2025性别图\",\"type\":10,\"template\":\"107.docx\",\"config\":[]}");
        put(108, "{\"name\":\"双学历广东省就业地区分布\",\"type\":10,\"template\":\"108.docx\",\"config\":[]}");
        put(109, "{\"name\":\"柱形图-薪酬涨幅\",\"type\":4,\"template\":\"109.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(110, "{\"name\":\"中长期-省内就业\",\"type\":3,\"template\":\"110.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1}]]}");
        put(111, "{\"name\":\"折线图-学院月均收入\",\"type\":1,\"template\":\"111.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(112, "{\"name\":\"条形图-学院薪酬\",\"type\":3,\"updateH\":true,\"height\":63,\"template\":\"112.docx\",\"config\":[[{\"type\":2,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
        put(113, "{\"name\":\"堆叠条形图-3列\",\"type\":3,\"updateH\":true,\"height\":90,\"template\":\"113.docx\",\"config\":[[{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":1,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":2,\"extCol\":-1},{\"type\":3,\"updateTx\":true,\"category\":0,\"values\":3,\"extCol\":-1}]]}");
        put(114, "{\"name\":\"柱状图-5柱2行-坐标自动\",\"type\":4,\"template\":\"114.docx\",\"config\":[]}");
        put(115, "{\"name\":\"柱状图-总体求职投入与收获\",\"type\":4,\"template\":\"115.docx\",\"config\":[[{\"type\":3,\"updateTx\":false,\"category\":0,\"values\":1,\"extCol\":-1}]]}");
    }};
}