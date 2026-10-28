package com.mowtiie.dueit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    public boolean register(String username, String email, String password) {
        try (SQLiteDatabase database = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("email", email);
            values.put("password", password);
            long result = database.insert("students", null, values);
            return result != -1;
        }
    }

    public boolean login(String username, String password) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            Cursor cursor = database.rawQuery("SELECT * FROM students WHERE username = ? AND password = ?", new String[]{username, password});
            int count = cursor.getCount();
            cursor.close();
            return count > 0;
        }
    }

    public ArrayList<Course> getCoursesForUser(String username) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            ArrayList<Course> courses = new ArrayList<>();

            String query = "SELECT c.id, c.user_id, c.course_name, c.instructor, c.schedule " +
                    "FROM courses c " +
                    "JOIN students s ON c.user_id = s.id " +
                    "WHERE s.username = ?";

            Cursor cursor = database.rawQuery(query, new String[]{username});
            if (cursor.moveToFirst()) {
                do {
                    Course course = new Course();
                    course.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    course.studentId = cursor.getInt(cursor.getColumnIndexOrThrow("student_id"));
                    course.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    course.instructor = cursor.getString(cursor.getColumnIndexOrThrow("instructor"));
                    course.schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
                    courses.add(course);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return courses;
        }
    }

    public ArrayList<Assignment> getAssignmentsFromCourse(int courseId) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            ArrayList<Assignment> assignments = new ArrayList<>();
            Cursor cursor = database.rawQuery("SELECT * FROM assignments WHERE course_id = ?", new String[]{String.valueOf(courseId)});

            if (cursor.moveToFirst()) {
                do {
                    Assignment assignment = new Assignment();
                    assignment.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    assignment.courseId = cursor.getInt(cursor.getColumnIndexOrThrow("course_id"));
                    assignment.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    assignment.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                    assignment.dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
                    assignment.priorityLevel = cursor.getString(cursor.getColumnIndexOrThrow("priority_level"));
                    assignment.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    assignments.add(assignment);
                } while (cursor.moveToNext());
            }
            cursor.close();
            return assignments;
        }
    }
}