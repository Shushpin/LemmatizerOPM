package ua.lemmatizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.uk.UkrainianMorfologikAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UkrainianLemmatizer {

    private final Analyzer analyzer;
    private final Pattern wordPattern = Pattern.compile("([а-яА-ЯіІїЇєЄґҐ'’]+)|([^а-яА-ЯіІїЇєЄґҐ'’]+)");

    // Словник "безумовних" винятків (де контекст не потрібен)
    private static final Map<String, String> EXCEPTIONS = new HashMap<>();

    static {
        EXCEPTIONS.put("мене", "я"); EXCEPTIONS.put("мені", "я"); EXCEPTIONS.put("мною", "я");
        EXCEPTIONS.put("тебе", "ти"); EXCEPTIONS.put("тобі", "ти"); EXCEPTIONS.put("тобою", "ти");
        EXCEPTIONS.put("його", "він"); EXCEPTIONS.put("йому", "він"); EXCEPTIONS.put("ним", "він");
        EXCEPTIONS.put("її", "вона"); EXCEPTIONS.put("їй", "вона");
        EXCEPTIONS.put("нас", "ми"); EXCEPTIONS.put("нам", "ми");
        EXCEPTIONS.put("вас", "ви"); EXCEPTIONS.put("вам", "ви");
        EXCEPTIONS.put("їх", "вони"); EXCEPTIONS.put("їм", "вони");
        EXCEPTIONS.put("друзі", "друг"); EXCEPTIONS.put("друзям", "друг");
        EXCEPTIONS.put("люди", "людина"); EXCEPTIONS.put("людей", "людина");
        EXCEPTIONS.put("діти", "дитина"); EXCEPTIONS.put("дітей", "дитина");
        EXCEPTIONS.put("різні", "різний"); EXCEPTIONS.put("різних", "різний");
    }

    public UkrainianLemmatizer() {
        this.analyzer = new UkrainianMorfologikAnalyzer(CharArraySet.EMPTY_SET);
    }

    public String lemmatize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = wordPattern.matcher(text);

        // Змінна для збереження попереднього слова
        String previousWord = "";

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                String currentWord = matcher.group(1);
                // Тепер передаємо поточне і попереднє слово
                result.append(getLemmaForWord(currentWord, previousWord));
                // Оновлюємо попереднє слово
                previousWord = currentWord.toLowerCase();
            } else if (matcher.group(2) != null) {
            // Додаємо toLowerCase() сюди, щоб навіть пробіли, цифри та іноземні слова ставали дрібними
            result.append(matcher.group(2).toLowerCase());
        }
        }

        return result.toString();
    }

    private String getLemmaForWord(String word, String prevWord) {
        // Одразу переводимо оригінальне слово в нижній регістр
        String lowerWord = word.toLowerCase();

        // 1. КОНТЕКСТНІ ПРАВИЛА (if-else)
        String contextualLemma = applyContextRules(lowerWord, prevWord);
        if (contextualLemma != null) {
            return contextualLemma.toLowerCase();
        }

        // 2. БЕЗУМОВНІ ВИНЯТКИ
        if (EXCEPTIONS.containsKey(lowerWord)) {
            return EXCEPTIONS.get(lowerWord).toLowerCase();
        }

        // 3. СТАНДАРТНИЙ LUCENE
        try (TokenStream stream = analyzer.tokenStream("dummy", new StringReader(word))) {
            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            if (stream.incrementToken()) {
                String lemma = termAtt.toString();
                stream.end();
                // Повертаємо лему виключно в нижньому регістрі
                return lemma.toLowerCase();
            }
            stream.end();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Якщо нічого не знайшли, повертаємо оригінальне слово, але теж маленьке
        return lowerWord;
    }

    // МЕТОД ДЛЯ IF-ELSE ЛОГІКИ НА ОСНОВІ ПОПЕРЕДНЬОГО СЛОВА
    private String applyContextRules(String word, String prevWord) {
        if (word.equals("став")) {
            // Якщо перед "став" іде прийменник місця/напрямку -> це ставок
            if (prevWord.matches("на|у|в|біля|до|через|про|за|над|під")) {
                return "ставок";
            }
            // У всіх інших випадках (я став, він став, друг став) -> це дієслово
            return "стати";
        }

        if (word.equals("поле")) {
            // Якщо перед "поле" йде дієслово, яке вказує на дію (він поле город) -> полоти
            if (prevWord.matches("він|вона|воно|батько|дід|я")) {
                return "полоти";
            }
            return "поле"; // Іменник (агрономічне поле)
        }

        // Можна додавати свої правила сюди...
        return null; // Якщо правил для слова немає, повертаємо null
    }
}