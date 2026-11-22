package com.example.interfaceuser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class VocabularyDbHelper extends SQLiteOpenHelper {

    public VocabularyDbHelper(Context context) {
        super(context.getApplicationContext(), "vocabulary.db", null, 1);
    }

    // DB에서 읽어 온 단어
    public static final class SavedWord {
        public final long id;
        public final String baseForm;
        public final String partOfSpeech;
        public final String meaning;

        public SavedWord(
                long id,
                String baseForm,
                String partOfSpeech,
                String meaning
        ) {
            this.id = id;
            this.baseForm = baseForm;
            this.partOfSpeech = partOfSpeech;
            this.meaning = meaning;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE words (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "base_form TEXT NOT NULL, " +
                        "part_of_speech TEXT NOT NULL, " +
                        "meaning TEXT NOT NULL DEFAULT '', " +
                        "UNIQUE(base_form, part_of_speech)" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("DB 버전 변경 코드가 필요합니다.");
    }

    // 선택한 단어 저장: 동일한 기본형 + 품사는 중복 저장x
    public int addWords(List<JapaneseWordSplitter.Word> words) {
        SQLiteDatabase db = getWritableDatabase();
        int addedCount = 0;

        db.beginTransaction();

        try {
            for (JapaneseWordSplitter.Word word : words) {
                ContentValues values = new ContentValues();
                values.put("base_form", word.baseForm);
                values.put("part_of_speech", word.partOfSpeech);
                values.put("meaning", "");

                long result = db.insertWithOnConflict(
                        "words",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_IGNORE
                );

                if (result != -1) {
                    addedCount++;
                }
            }

            db.setTransactionSuccessful();
            return addedCount;

        } finally {
            db.endTransaction();
        }
    }

    // 저장된 단어장 전체 조회
    public List<SavedWord> getAllWords() {
        List<SavedWord> words = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT id, base_form, part_of_speech, meaning " +
                        "FROM words ORDER BY id ASC",
                null
        )) {
            while (cursor.moveToNext()) {
                words.add(new SavedWord(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3)
                ));
            }
        }

        return words;
    }
}