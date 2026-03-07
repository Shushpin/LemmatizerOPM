package ua.lemmatizer;

public class TextCleaner {

    public static String removePunctuation(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Залишаємо ТІЛЬКИ літери (\p{L}), цифри (\p{Nd}), пробіли (\s) та апострофи (' і ’)
        // Усе інше (будь-які знаки пунктуації з будь-яких мов) замінюємо на порожнечу
        return text.replaceAll("[^\\p{L}\\p{Nd}\\s'’]+", "");
    }
}