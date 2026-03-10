package ua.lemmatizer;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.Properties;
import java.util.stream.Collectors;

public class EnglishLemmatizer {

    private final StanfordCoreNLP pipeline;

    public EnglishLemmatizer() {
        System.out.println("Завантаження моделей NLP... (це може зайняти кілька секунд)");
        Properties props = new Properties();
        // Вказуємо, що нам потрібна токенізація, визначення частин мови (pos) та лематизація
        props.setProperty("annotators", "tokenize,pos,lemma");
        this.pipeline = new StanfordCoreNLP(props);
        System.out.println("Моделі завантажено!");
    }

    public String lemmatize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);

        // Проходимось по всіх токенах і примусово робимо їх маленькими (toLowerCase)
        return document.tokens().stream()
                .map(token -> token.lemma().toLowerCase())
                .collect(Collectors.joining(" "));
    }

}