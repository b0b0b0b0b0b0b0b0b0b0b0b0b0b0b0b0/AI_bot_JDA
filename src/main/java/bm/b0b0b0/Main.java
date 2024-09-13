package bm.b0b0b0;

import bm.b0b0b0.util.Config;

public class Main {
    public static void main(String[] args) {
        // Загружаем конфиг при старте
        Config config = ConfigLoader.getConfig();

        // Создаем и запускаем бота, передаем конфиг в конструктор
        DiscordBot bot = new DiscordBot(config);
        try {
            bot.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
