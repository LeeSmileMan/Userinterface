package com.example.interfaceuser;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final JapaneseWordSplitter splitter =
            new JapaneseWordSplitter();

    private String toKoreanPartOfSpeech(String pos) {
        if (pos == null) {
            return "기타";
        }

        switch (pos) { // kuromoji에서 한자로 받은 결과 한글로 변환
            case "名詞": return "명사";
            case "動詞": return "동사";
            case "形容詞": return "형용사";
            case "副詞": return "부사";
            case "助詞": return "조사";
            case "助動詞": return "조동사";
            case "連体詞": return "관형사";
            case "接続詞": return "접속사";
            case "感動詞": return "감탄사";
            case "接頭詞": return "접두사";
            case "記号": return "기호";
            default: return "기타";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText inputText = findViewById(R.id.inputText);
        Button splitButton = findViewById(R.id.splitButton);
        TextView resultText = findViewById(R.id.resultText);

        splitButton.setOnClickListener(view -> {
            String text = inputText.getText().toString();

            if (text.trim().isEmpty()) { // 비어있을경우
                inputText.setError("일본어 글을 입력해주세요.");
                return;
            }

            splitButton.setEnabled(false);
            resultText.setText("분석 중입니다.");

            executor.execute(() -> {
                    List<JapaneseWordSplitter.Word> words =
                            splitter.extractVocabulary(text);

                    StringBuilder output = new StringBuilder();

                for (JapaneseWordSplitter.Word word : words) {
                    output.append(word.baseForm)
                            .append(" [")
                            .append(toKoreanPartOfSpeech(word.partOfSpeech))
                            .append("]\n")
                            .append("뜻 : ")
                            .append("\n\n");
                }

                    String result = words.isEmpty()
                            ? "추출된 단어가 없습니다."
                            : output.toString();

                    // 아직 SQLite를 이용한 단어장 구현 x , 추후에 추출한 단어 단어장으로 이동 추가

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        resultText.setText(result);
                        splitButton.setEnabled(true);
                    });
            });
        });
    }
// 숫자나 영어 입력시 값이 그대로 나오는 경우 수정 필요
    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}