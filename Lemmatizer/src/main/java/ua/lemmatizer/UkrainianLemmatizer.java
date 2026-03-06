package ua.lemmatizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.uk.UkrainianMorfologikAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UkrainianLemmatizer {

    private final Analyzer analyzer;
    // Регулярний вираз: Група 1 (українські літери та апостроф) АБО Група 2 (все інше - знаки, пробіли, цифри)
    private final Pattern wordPattern = Pattern.compile("([а-яА-ЯіІїЇєЄґҐ']+)|([^а-яА-ЯіІїЇєЄґҐ']+)");

    public UkrainianLemmatizer() {
        this.analyzer = new UkrainianMorfologikAnalyzer(CharArraySet.EMPTY_SET);
    }

    public String lemmatize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = wordPattern.matcher(text);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Це слово – дістаємо його лему
                result.append(getLemmaForWord(matcher.group(1)));
            } else if (matcher.group(2) != null) {
                // Це розділові знаки, пробіли чи перенесення рядка – залишаємо як є
                result.append(matcher.group(2));
            }
        }

        return result.toString();
    }

    private String getLemmaForWord(String word) {
        try (TokenStream stream = analyzer.tokenStream("dummy", new StringReader(word))) {
            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            // Беремо лише першу лему, автоматично відкидаючи омоніми
            if (stream.incrementToken()) {
                String lemma = termAtt.toString();

                // Якщо оригінальне слово було з великої літери (напр. на початку речення),
                // робимо лему теж з великої літери
                if (Character.isUpperCase(word.charAt(0)) && !lemma.isEmpty()) {
                    lemma = lemma.substring(0, 1).toUpperCase() + lemma.substring(1);
                }

                stream.end();
                return lemma;
            }
            stream.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return word; // Якщо щось пішло не так, повертаємо як було
    }
}