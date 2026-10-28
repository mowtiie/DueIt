package com.mowtiie.dueit.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;

public class DueItDatabase extends SQLiteOpenHelper {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_COMPLETED = "Completed";

    public DueItDatabase(@Nullable Context context) {
        super(context, "dueit.db", null, 2);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS students(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE," +
                "email TEXT NOT NULL," +
                "password TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS courses(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL REFERENCES students(id) ON DELETE CASCADE," +
                "name TEXT NOT NULL," +
                "instructor TEXT NOT NULL," +
                "schedule TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS assignments(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE," +
                "title TEXT NOT NULL," +
                "due_date TEXT NOT NULL," +
                "description TEXT NOT NULL DEFAULT ''," +
                "priority_level TEXT NOT NULL," +
                "status TEXT NOT NULL DEFAULT 'Pending')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS assignments;");
        db.execSQL("DROP TABLE IF EXISTS courses;");
        db.execSQL("DROP TABLE IF EXISTS students;");
        onCreate(db);
    }

    // Students

    public boolean register(String username, String email, String password) {
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("email", email);
        values.put("password", hashPassword(password));
        return getWritableDatabase().insert("students", null, values) != -1;
    }

    /** Returns the student's id, or -1 when the credentials are wrong. */
    public int login(String username, String password) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, password FROM students WHERE username = ?", new String[]{username})) {
            if (cursor.moveToFirst() && verifyPassword(password, cursor.getString(1))) {
                return cursor.getInt(0);
            }
            return -1;
        }
    }

    private static String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltHex = toHex(salt);
        return saltHex + "$" + sha256(saltHex + password);
    }

    private static boolean verifyPassword(String password, String stored) {
        String[] parts = stored.split("\\$");
        return parts.length == 2 && sha256(parts[0] + password).equals(parts[1]);
    }

    private static String sha256(String input) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) builder.append(String.format("%02x", b));
        return builder.toString();
    }

    // Courses

    public boolean addCourse(int studentId, String name, String instructor, String schedule) {
        ContentValues values = new ContentValues();
        values.put("student_id", studentId);
        values.put("name", name);
        values.put("instructor", instructor);
        values.put("schedule", schedule);
        return getWritableDatabase().insert("courses", null, values) != -1;
    }

    public boolean updateCourse(int id, String name, String instructor, String schedule) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("instructor", instructor);
        values.put("schedule", schedule);
        return getWritableDatabase().update("courses", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public void deleteCourse(int id) {
        getWritableDatabase().delete("courses", "id = ?", new String[]{String.valueOf(id)});
    }

    @Nullable
    public Course getCourse(int id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM courses WHERE id = ?", new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? readCourse(cursor) : null;
        }
    }

    public ArrayList<Course> getCourses(int studentId) {
        ArrayList<Course> courses = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM courses WHERE student_id = ? ORDER BY name COLLATE NOCASE",
                new String[]{String.valueOf(studentId)})) {
            while (cursor.moveToNext()) courses.add(readCourse(cursor));
        }
        return courses;
    }

    private Course readCourse(Cursor cursor) {
        Course course = new Course();
        course.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        course.studentId = cursor.getInt(cursor.getColumnIndexOrThrow("student_id"));
        course.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        course.instructor = cursor.getString(cursor.getColumnIndexOrThrow("instructor"));
        course.schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
        return course;
    }

    // Assignments

    public boolean addAssignment(int courseId, String title, String dueDate, String description, String priorityLevel) {
        ContentValues values = new ContentValues();
        values.put("course_id", courseId);
        values.put("title", title);
        values.put("due_date", dueDate);
        values.put("description", description);
        values.put("priority_level", priorityLevel);
        return getWritableDatabase().insert("assignments", null, values) != -1;
    }

    public boolean updateAssignment(int id, String title, String dueDate, String description, String priorityLevel) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("due_date", dueDate);
        values.put("description", description);
        values.put("priority_level", priorityLevel);
        return getWritableDatabase().update("assignments", values, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public void updateAssignmentStatus(int id, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        getWritableDatabase().update("assignments", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteAssignment(int id) {
        getWritableDatabase().delete("assignments", "id = ?", new String[]{String.valueOf(id)});
    }

    @Nullable
    public Assignment getAssignment(int id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM assignments WHERE id = ?", new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? readAssignment(cursor) : null;
        }
    }

    public ArrayList<Assignment> getAssignmentsFromCourse(int courseId) {
        ArrayList<Assignment> assignments = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT * FROM assignments WHERE course_id = ? ORDER BY due_date",
                new String[]{String.valueOf(courseId)})) {
            while (cursor.moveToNext()) assignments.add(readAssignment(cursor));
        }
        return assignments;
    }

    /** Returns {completed, total} assignment counts for a course. */
    public int[] getCourseProgress(int courseId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(status = ?), 0), COUNT(*) FROM assignments WHERE course_id = ?",
                new String[]{STATUS_COMPLETED, String.valueOf(courseId)})) {
            cursor.moveToFirst();
            return new int[]{cursor.getInt(0), cursor.getInt(1)};
        }
    }

    private Assignment readAssignment(Cursor cursor) {
        Assignment assignment = new Assignment();
        assignment.id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        assignment.courseId = cursor.getInt(cursor.getColumnIndexOrThrow("course_id"));
        assignment.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        assignment.description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        assignment.dueDate = cursor.getString(cursor.getColumnIndexOrThrow("due_date"));
        assignment.priorityLevel = cursor.getString(cursor.getColumnIndexOrThrow("priority_level"));
        assignment.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
        return assignment;
    }
}
