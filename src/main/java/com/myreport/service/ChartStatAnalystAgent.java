package com.myreport.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * 图表统计分析师 Agent（DashScope qwen-plus）。
 * 供 {@code SpireReportUtil.insertChartText} 静态调用。
 */
@Component
public class ChartStatAnalystAgent {

    private static final Logger logger = Logger.getLogger(ChartStatAnalystAgent.class);

    private static final int MAX_CHARS = 200;

    private static final String SYSTEM_PROMPT =
            "你是一名专业的统计分析师。请根据用户提供的图表/统计数据（strData），"
                    + "给出简洁、合理的解读与见解。要求：总字数不超过200字；"
                    + "只输出分析正文，不要标题、编号列表或开场套话；"
                    + "结论须有数据依据，避免臆造未给出的数字。";

    private static volatile ChartStatAnalystAgent INSTANCE;

    @Value("${myreport.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${myreport.ai.dashscope.model:qwen-plus}")
    private String model;

    @Value("${myreport.ai.chart-analyst.enabled:true}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        INSTANCE = this;
    }

    /**
     * 以 strData 为上下文生成图表分析文案；失败或未配置时返回 null。
     */
    public static String analyze(String strData) {
        ChartStatAnalystAgent agent = INSTANCE;
        if (agent == null) {
            return null;
        }
        return agent.doAnalyze(strData);
    }

    private String doAnalyze(String strData) {
        if (!enabled || StringUtils.isBlank(apiKey) || StringUtils.isBlank(strData)) {
            return null;
        }
        try {
            Generation gen = new Generation();
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(SYSTEM_PROMPT)
                    .build();
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content("以下为图表数据上下文（strData），请给出分析：\n" + strData)
                    .build();
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey.trim())
                    .model(StringUtils.isBlank(model) ? "qwen-plus" : model.trim())
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .maxTokens(280)
                    .build();
            GenerationResult result = gen.call(param);
            if (result == null || result.getOutput() == null
                    || result.getOutput().getChoices() == null
                    || result.getOutput().getChoices().isEmpty()
                    || result.getOutput().getChoices().get(0).getMessage() == null) {
                return null;
            }
            String content = result.getOutput().getChoices().get(0).getMessage().getContent();
            return truncateToMaxChars(content, MAX_CHARS);
        } catch (Exception e) {
            logger.error("chart stat analyst agent failed", e);
            return null;
        }
    }

    static String truncateToMaxChars(String text, int maxChars) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars);
    }
}
