package bm.b0b0b0.ai;

import bm.b0b0b0.ConfigLoader;
import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ApiService {
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);
    private final OkHttpClient client;
    private final String aiToken;
    private final String modelId;

    public ApiService() {
        this.client = new OkHttpClient();
        this.aiToken = ConfigLoader.getConfig().getAiToken();
        this.modelId = ConfigLoader.getConfig().getModelId();
    }

    public String askAI(String userMessage) {
        logger.info("Отправка запроса к AI с сообщением пользователя: {}", userMessage);

        // Формируем тело запроса корректно, с экранированием символов
        String requestBody = String.format(
                "{ \"model\": \"%s\", \"messages\": [" +
                        "{\"role\": \"system\", \"content\": \"Ты ассистент.\"}," +
                        "{\"role\": \"user\", \"content\": \"%s\"}" +
                        "] }",
                modelId,
                escapeJson(userMessage)
        );

        RequestBody body = RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + aiToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return handleApiError(response);
            }

            String responseBody = response.body() != null ? response.body().string() : null;
            logger.info("Ответ успешно получен от AI.");
            return extractMessageFromResponse(responseBody);
        } catch (IOException e) {
            logger.error("Ошибка соединения с AI.", e);
            return "Ошибка соединения с AI.";
        }
    }

    private String handleApiError(Response response) throws IOException {
        int statusCode = response.code();
        String errorBody = response.body() != null ? response.body().string() : "Нет тела ответа";

        logger.error("Ошибка запроса к AI. Код ответа: {}, тело ответа: {}", statusCode, errorBody);

        switch (statusCode) {
            case 429:
                logger.warn("Превышен лимит скорости. Код ошибки: 429");
                return "Ошибка 429: Превышен лимит скорости. Пожалуйста, повторите попытку позже.";
            case 502:
                return "Кажется, GPT устроило забастовку. Пожалуйста, попробуйте позже. Код ошибки: **502**";
            case 503:
                return "Сервера GPT временно недоступны. Попробуйте позже. Код ошибки: **503**";
            case 504:
                return "GPT взял перерыв. Пожалуйста, попробуйте позже. Код ошибки: **504**";
            case 404:
                return "GPT не нашёл информацию. Код ошибки: **404**";
            case 400:
                return "Неверный запрос. Пожалуйста, проверьте данные. Код ошибки: **400**";
            default:
                return "Неизвестная ошибка при запросе к AI. Код ошибки: " + statusCode;
        }
    }

    // Метод для экранирования спецсимволов в JSON
    private String escapeJson(String text) {
        return text.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String extractMessageFromResponse(String responseBody) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
        JsonArray choices = jsonObject.getAsJsonArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            JsonElement content = message.get("content");
            if (content != null) {
                return content.getAsString().trim();
            }
        }
        logger.warn("Не удалось получить ответ от AI.");
        return "Не удалось получить ответ от AI.";
    }
}
