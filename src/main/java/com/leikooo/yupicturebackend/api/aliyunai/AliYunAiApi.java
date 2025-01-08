package com.leikooo.yupicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.leikooo.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.leikooo.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.leikooo.yupicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.leikooo.yupicturebackend.exception.BusinessException;
import com.leikooo.yupicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2025/01/09
 */
@Slf4j
@Component
public class AliYunAiApi {

    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(CREATE_OUT_PAINTING_TASK_URL);
            httpPost.addHeader("X-DashScope-Async", "enable");
            httpPost.addHeader("Authorization", "Bearer " + apiKey);
            httpPost.addHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(JSONUtil.toJsonStr(createOutPaintingTaskRequest), "UTF-8");
            httpPost.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (response.getStatusLine().getStatusCode() != 200) {
                log.error("请求异常：{}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }

            CreateOutPaintingTaskResponse paintingResponse = JSONUtil.toBean(responseBody, CreateOutPaintingTaskResponse.class);
            String errorCode = paintingResponse.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = paintingResponse.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return paintingResponse;
        } catch (IOException e) {
            log.error("创建扩图任务时发生错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误，请稍后再试");
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(String.format(GET_OUT_PAINTING_TASK_URL, taskId));
            httpGet.addHeader("Authorization", "Bearer " + apiKey);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
            if (response.getStatusLine().getStatusCode() != 200) {
                log.error("请求异常：{}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(responseBody, GetOutPaintingTaskResponse.class);
        } catch (IOException e) {
            log.error("获取图片 task 信息发生错误", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误，请稍后再试");
        }
    }
}
