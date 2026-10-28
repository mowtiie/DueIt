package com.mowtiie.dueit.ui.course;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.mowtiie.dueit.data.database.DueItDatabase;
import com.mowtiie.dueit.data.model.Course;
import com.mowtiie.dueit.databinding.ActivityManageCourseBinding;

import java.util.Objects;

public class ManageCourseActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID = "EXTRA_STUDENT_ID";
    public static final String EXTRA_COURSE_ID = "EXTRA_COURSE_ID";

    ActivityManageCourseBinding binding;
    DueItDatabase database;
    int studentId;
    int courseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityManageCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = new DueItDatabase(this);
        studentId = getIntent().getIntExtra(EXTRA_STUDENT_ID, -1);
        courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, 0);
        boolean editing = courseId != 0;

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(editing ? "Edit course" : "Add course");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (editing) {
            Course course = database.getCourse(courseId);
            if (course == null) {
                finish();
                return;
            }
            binding.nameInput.setText(course.name);
            binding.instructorInput.setText(course.instructor);
            binding.scheduleInput.setText(course.schedule);
            binding.btnDelete.setVisibility(View.VISIBLE);
        }

        binding.btnSave.setOnClickListener(v -> save(editing));
        binding.btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Delete course?")
                .setMessage("This also deletes all of its assignments.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    database.deleteCourse(courseId);
                    finish();
                })
                .show());
    }

    private void save(boolean editing) {
        String name = Objects.requireNonNull(binding.nameInput.getText()).toString().trim();
        String instructor = Objects.requireNonNull(binding.instructorInput.getText()).toString().trim();
        String schedule = Objects.requireNonNull(binding.scheduleInput.getText()).toString().trim();

        if (name.isEmpty() || instructor.isEmpty() || schedule.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (name.length() > 100 || instructor.length() > 100 || schedule.length() > 100) {
            Toast.makeText(this, "Each field must be 100 characters or less!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved = editing
                ? database.updateCourse(courseId, name, instructor, schedule)
                : database.addCourse(studentId, name, instructor, schedule);
        if (saved) {
            finish();
        } else {
            Toast.makeText(this, "Could not save the course", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
