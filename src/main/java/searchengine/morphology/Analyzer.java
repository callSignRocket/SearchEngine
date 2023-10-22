package searchengine.morphology;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class Analyzer implements Morphology{

    private static RussianLuceneMorphology russianLuceneMorphology;
    private static EnglishLuceneMorphology englishLuceneMorphology;
    private static final String REGEX = "[\\p{Punct}\\d@©◄»«—№…]";
    private final static Marker INVALID_SYMBOL_MARKER = MarkerManager.getMarker("INVALID_SYMBOL");
    private final static Logger LOGGER = LogManager.getLogger(Analyzer.class);

    static {
        try {
            russianLuceneMorphology = new RussianLuceneMorphology();
            englishLuceneMorphology = new EnglishLuceneMorphology();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public HashMap<String, Integer> getLemmaList(String content) {
        content = content.toLowerCase(Locale.ROOT).replaceAll(REGEX, " ");
        HashMap<String, Integer> lemmaList = new HashMap<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\s+");
        for (String element : elements) {
            List<String> wordsList = getLemma(element);
            for (String word : wordsList) {
                int count = lemmaList.getOrDefault(word, 0);
                lemmaList.put(word, count + 1);
            }
        }
        return lemmaList;
    }

    @Override
    public List<String> getLemma(String word) {
        List<String> lemmaList = new ArrayList<>();
        boolean checkLang = word.chars().mapToObj(Character.UnicodeBlock::of).anyMatch(s -> s.equals(Character.UnicodeBlock.CYRILLIC));
        try {
            if (checkLang) {
                List<String> baseRusForm = russianLuceneMorphology.getNormalForms(word);
                if (!isServiceWordRus(word)) {
                    lemmaList.addAll(baseRusForm);
                }
            } else {
                List<String> baseEngForm = englishLuceneMorphology.getNormalForms(word);
                if (!isServiceWordEng(word)) {
                    lemmaList.addAll(baseEngForm);
                }
            }
        } catch (Exception e) {
            LOGGER.debug(INVALID_SYMBOL_MARKER, "Символ не найден - {}", word);
        }
        return lemmaList;
    }

    @Override
    public List<Integer> findLemmaIndexInText(String content, String lemma) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("[\\p{Punct}\\s]");
        int index = 0;
        for (String element : elements) {
            List<String> lemmas = getLemma(element);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += element.length() + 1;
        }
        return lemmaIndexList;
    }

    private boolean isServiceWordRus(String word) {
        List<String> morphForm = russianLuceneMorphology.getMorphInfo(word);
        for (String l : morphForm) {
            if (l.contains("ПРЕДЛ") ||
                l.contains("СОЮЗ") ||
                l.contains("МЕЖД") ||
                l.contains("МС") ||
                l.contains("ЧАСТ") ||
                l.length() <= 3) {
                return true;
            }
        }
        return false;
    }

    private boolean isServiceWordEng(String word) {
        List<String> morphForm = englishLuceneMorphology.getMorphInfo(word);
        for (String l : morphForm) {
            if (l.contains("PREP") ||
                l.contains("CONJ") ||
                l.contains("INT") ||
                l.contains("ARTICLE")) {
                return true;
            }
        }
        return false;
    }
}
