package bm.b0b0b0.util;

public class Config {
    public final String botToken;
    public final String aiToken;
    public final String modelId;
    public final Channels channels;

    // Конструктор
    public Config(String botToken, String aiToken, String modelId, Channels channels) {
        this.botToken = botToken;
        this.aiToken = aiToken;
        this.modelId = modelId;
        this.channels = channels;
    }

    // Геттеры для полей
    public String getBotToken() {
        return botToken;
    }

    public String getAiToken() {
        return aiToken;
    }

    public String getModelId() {
        return modelId;
    }

    public Channels getChannels() {
        return channels;
    }

    // Вложенный класс для каналов
    public static class Channels {
        private final String general;
        private final String support;

        // Конструктор
        public Channels(String general, String support) {
            this.general = general;
            this.support = support;
        }

        // Геттеры для полей
        public String getGeneral() {
            return general;
        }

        public String getSupport() {
            return support;
        }
    }
}
