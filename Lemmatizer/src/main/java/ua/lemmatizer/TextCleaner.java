package ua.lemmatizer;

public class TextCleaner {

    // Метод статичний, бо він не зберігає ніякого стану
    public static String removePunctuation(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Замінюємо всі розділові знаки на порожнечу і переводимо в нижній регістр
        return text.replaceAll("\\p{Punct}", "").toLowerCase();
    }
}
