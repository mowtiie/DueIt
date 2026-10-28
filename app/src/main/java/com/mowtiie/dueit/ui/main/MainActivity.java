package com.mowtiie.dueit.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mowtiie.dueit.R;
import com.mowtiie.dueit.data.database.DueItDatabase;
import com.mowtiie.dueit.data.model.Course;
import com.mowtiie.dueit.databinding.ActivityMainBinding;
import com.mowtiie.dueit.ui.adapter.GenericAdapter;
import com.mowtiie.dueit.ui.assignment.AssignmentListActivity;
import com.mowtiie.dueit.ui.auth.LoginActivity;
import com.mowtiie.dueit.ui.course.ManageCourseActivity;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_ID = "EXTRA_STUDENT_ID";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";

    ActivityMainBinding binding;
    DueItDatabase database;
    GenericAdapter<Course> adapter;
    int studentId;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = new DueItDatabase(this);
        studentId = getIntent().getIntExtra(EXTRA_STUDENT_ID, -1);
        String username = getIntent().getStringExtra(EXTRA_USERNAME);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Welcome back, " + (username != null ? username : "student") + "!");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        adapter = new GenericAdapter<>(R.layout.item_course, new ArrayList<>(), (itemView, course, position) -> {
            ((android.widget.TextView) itemView.findViewById(R.id.course_name)).setText(course.name);
            ((android.widget.TextView) itemView.findViewById(R.id.course_instructor)).setText("Instructor: " + course.instructor);
            ((android.widget.TextView) itemView.findViewById(R.id.course_schedule)).setText("Schedule: " + course.schedule);

            int[] progress = database.getCourseProgress(course.id);
            int percent = progress[1] == 0 ? 0 : progress[0] * 100 / progress[1];
            ((android.widget.TextView) itemView.findViewById(R.id.course_progress)).setText(
                    String.format(Locale.getDefault(), "%d of %d assignments completed (%d%%)", progress[0], progress[1], percent));
            ((com.google.android.material.progressindicator.LinearProgressIndicator) itemView.findViewById(R.id.course_progress_bar)).setProgress(percent);

            itemView.findViewById(R.id.btn_edit).setOnClickListener(v -> openManageCourse(course.id));
        });
        adapter.setOnItemClickListener(course -> {
            Intent intent = new Intent(this, AssignmentListActivity.class);
            intent.putExtra(AssignmentListActivity.EXTRA_COURSE_ID, course.id);
            intent.putExtra(AssignmentListActivity.EXTRA_COURSE_NAME, course.name);
            startActivity(intent);
        });

        binding.courseList.setLayoutManager(new LinearLayoutManager(this));
        binding.courseList.setAdapter(adapter);
        binding.btnAddCourse.setOnClickListener(v -> openManageCourse(0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ArrayList<Course> courses = database.getCourses(studentId);
        adapter.updateList(courses);
        binding.emptyIndicator.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openManageCourse(int courseId) {
        Intent intent = new Intent(this, ManageCourseActivity.class);
        intent.putExtra(ManageCourseActivity.EXTRA_STUDENT_ID, studentId);
        intent.putExtra(ManageCourseActivity.EXTRA_COURSE_ID, courseId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
