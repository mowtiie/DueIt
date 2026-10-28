package com.mowtiie.dueit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DueItDatabase extends SQLiteOpenHelper {

    public DueItDatabase(@Nullable Context context) {
        super(context, "dueit.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS students(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL," +
                "email TEXT NOT NULL," +
                "password TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS courses(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "instructor TEXT NOT NULL," +
                "schedule TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS assignments(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "course_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "due_date TEXT NOT NULL," +
                "description TEXT NOT NULL," +
                "priority_level TEXT NOT NULL," +
                "status TEXT NOT NULL DEFAULT 'Pending')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS students;");
        db.execSQL("DROP TABLE IF EXISTS courses;");
        db.execSQL("DROP TABLE IF EXISTS assignments;");
    }
}