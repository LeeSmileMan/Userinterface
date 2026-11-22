package com.example.interfaceuser;

import android.app.AlertDialog;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private EditText inputText;
    private Button splitButton;
    private TextView resultText;
    private VocabularyDbHelper dbHelper;

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

        inputText = findViewById(R.id.inputText);
        splitButton = findViewById(R.id.splitButton);
        resultText = findViewById(R.id.resultText);

        dbHelper = new VocabularyDbHelper(this);

// 앱을 열면 기존에 저장된 단어장을 표시
        loadVocabulary();

        splitButton.setText("단어 추출 및 선택");

        splitButton.setOnClickListener(view -> {

            String text = inputText.getText().toString();

            if (text.trim().isEmpty()) {
                inputText.setError("일본어 글을 입력해주세요.");
                return;
            }

            splitButton.setEnabled(false);

            executor.execute(() -> {
                // 입력한 글에서 단어 추출
                List<JapaneseWordSplitter.Word> words =
                        splitter.extractVocabulary(text);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    splitButton.setEnabled(true);

                    if (words.isEmpty()) {
                        Toast.makeText(
                                MainActivity.this,
                                "추출된 단어가 없습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // 저장할 단어를 선택하는 창 표시
                    // 실제 SQLite 저장은 선택 창의 저장 버튼에서 실행
                    showWordSelectionDialog(words);
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

    private void loadVocabulary() {
        splitButton.setEnabled(false);

        executor.execute(() -> {
                List<VocabularyDbHelper.SavedWord> words =
                        dbHelper.getAllWords();

                String output = formatVocabulary(words);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    resultText.setText(output);
                    splitButton.setEnabled(true);
                });


        });
    }

    private void showWordSelectionDialog(
            List<JapaneseWordSplitter.Word> words
    ) {
        String[] labels = new String[words.size()];
        boolean[] checked = new boolean[words.size()];

        for (int i = 0; i < words.size(); i++) {
            JapaneseWordSplitter.Word word = words.get(i);

            labels[i] = word.baseForm
                    + " ["
                    + toKoreanPartOfSpeech(word.partOfSpeech)
                    + "]";

            checked[i] = true;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("저장할 단어 선택")
                .setMultiChoiceItems(
                        labels,
                        checked,
                        (dialogInterface, which, isChecked) ->
                                checked[which] = isChecked
                )
                .setNegativeButton("취소", null)
                .setPositiveButton("선택한 단어 저장", null)
                .create();

        dialog.setOnShowListener(unused -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {
                        List<JapaneseWordSplitter.Word> selectedWords =
                                new ArrayList<>();

                        for (int i = 0; i < words.size(); i++) {
                            if (checked[i]) {
                                selectedWords.add(words.get(i));
                            }
                        }

                        if (selectedWords.isEmpty()) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "저장할 단어를 하나 이상 선택해주세요.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        dialog.dismiss();
                        saveSelectedWords(selectedWords);
                    });
        });

        dialog.show();
    }
    private String formatVocabulary(
            List<VocabularyDbHelper.SavedWord> words
    ) {
        if (words.isEmpty()) {
            return "아직 저장된 단어가 없습니다.";
        }

        StringBuilder output = new StringBuilder();

        output.append("내 단어장 · ")
                .append(words.size())
                .append("개\n\n");

        for (VocabularyDbHelper.SavedWord word : words) {
            output.append(word.baseForm)
                    .append(" [")
                    .append(toKoreanPartOfSpeech(word.partOfSpeech))
                    .append("]\n")
                    .append("뜻 : ")
                    .append(word.meaning)
                    .append("\n\n");
        }

        return output.toString();
    }
    private void saveSelectedWords(
            List<JapaneseWordSplitter.Word> selectedWords
    ) {
        splitButton.setEnabled(false);

        executor.execute(() -> {
                // 선택한 단어 저장
                int addedCount = dbHelper.addWords(selectedWords);

                // 저장 후 단어장 전체 조회
                List<VocabularyDbHelper.SavedWord> savedWords =
                        dbHelper.getAllWords();

                String output = formatVocabulary(savedWords);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    resultText.setText(output);
                    splitButton.setEnabled(true);

                    String message = addedCount == 0
                            ? "새로 추가된 단어가 없습니다."
                            : addedCount + "개 단어를 저장했습니다.";

                    Toast.makeText(
                            MainActivity.this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show();
                });
        });
    }

}