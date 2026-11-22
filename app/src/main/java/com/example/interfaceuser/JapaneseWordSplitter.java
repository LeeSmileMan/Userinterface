package com.example.interfaceuser;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class JapaneseWordSplitter {



    private Tokenizer tokenizer;

    public static final class Word {
        public final String baseForm;
        public final String partOfSpeech;


        public Word(
                String baseForm,
                String partOfSpeech
        ) {
            this.baseForm = baseForm;
            this.partOfSpeech = partOfSpeech;
        }
    }

    /**
     * 조사·조동사를 포함해 분리
     * 공백과 문장부호는 제외
     */
    public synchronized List<Word> split(String text) {
        List<Word> result = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        if (tokenizer == null) {
            tokenizer = new Tokenizer();
        }

        for (Token token : tokenizer.tokenize(text)) {
            String surface = token.getSurface();
            String pos = token.getPartOfSpeechLevel1();

            if ("記号".equals(pos) || !containsLetterOrDigit(surface)) {
                continue;
            }

            String baseForm = token.getBaseForm();

            // 미등록어는 기본형이 없을 수 있으므로 원문을 사용합니다.
            if (isMissing(baseForm)) {
                baseForm = surface;
            }

            String reading = token.getReading();
            if (isMissing(reading)) {
                reading = "";
            }

            result.add(new Word(baseForm, pos));
        }

        return result;
    }

    /**
     * 단어장에 넣을 후보 추출
     * 중복 항목은 합치기
     */
    public List<Word> extractVocabulary(String text) {
        Map<String, Word> unique = new LinkedHashMap<>();

        for (Word word : split(text)) {
            if (!isVocabularyPartOfSpeech(word.partOfSpeech)) {
                continue;
            }

            String key = word.baseForm + "\u0000" + word.partOfSpeech;

            if (!unique.containsKey(key)) {
                unique.put(key, word);
            }
        }

        return new ArrayList<>(unique.values());
    }

    private static boolean isVocabularyPartOfSpeech(String pos) {
        return "名詞".equals(pos)       // 명사
                || "動詞".equals(pos)  // 동사
                || "形容詞".equals(pos) // 형용사
                || "副詞".equals(pos); // 부사
    }

    private static boolean isMissing(String value) {
        return value == null || value.isEmpty() || "*".equals(value);
    }

    private static boolean containsLetterOrDigit(String text) {
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);

            if (Character.isLetterOrDigit(codePoint)) {
                return true;
            }

            offset += Character.charCount(codePoint);
        }

        return false;
    }
}