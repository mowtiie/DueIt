package com.mowtiie.dueit.ui.assignment;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;

import com.mowtiie.dueit.R;
import com.mowtiie.dueit.data.database.DueItDatabase;
import com.mowtiie.dueit.data.model.Assignment;
import com.mowtiie.dueit.databinding.ActivityAssignmentListBinding;
import com.mowtiie.dueit.ui.adapter.GenericAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AssignmentListActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "EXTRA_COURSE_ID";
    public static final String EXTRA_COURSE_NAME = "EXTRA_COURSE_NAME";

    ActivityAssignmentListBinding binding;
    DueItDatabase database;
    GenericAdapter<Assignment> adapter;
    int courseId;
    String courseName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAssignmentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = new DueItDatabase(this);
        courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(courseName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new GenericAdapter<>(R.layout.item_assignment, new ArrayList<>(), (itemView, assignment, position) -> {
            boolean done = DueItDatabase.STATUS_COMPLETED.equals(assignment.status);
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

            TextView title = itemView.findViewById(R.id.assignment_title);
            title.setText(assignment.title);
            title.setPaintFlags(done
                    ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

            boolean overdue = !done && assignment.dueDate.compareTo(now) < 0;
            ((TextView) itemView.findViewById(R.id.assignment_due)).setText(
                    "Due " + assignment.dueDate + (overdue ? " (overdue)" : ""));

            TextView description = itemView.findViewById(R.id.assignment_description);
            description.setText(assignment.description);
            description.setVisibility(assignment.description.isEmpty() ? View.GONE : View.VISIBLE);

            ((Chip) itemView.findViewById(R.id.assignment_priority)).setText(assignment.priorityLevel);

            MaterialCheckBox checkBox = itemView.findViewById(R.id.assignment_done);
            checkBox.setChecked(done);
            checkBox.setOnClickListener(v -> {
                database.updateAssignmentStatus(assignment.id,
                        checkBox.isChecked() ? DueItDatabase.STATUS_COMPLETED : DueItDatabase.STATUS_PENDING);
                refresh();
            });
        });
        adapter.setOnItemClickListener(assignment -> openManageAssignment(assignment.id));

        binding.assignmentList.setLayoutManager(new LinearLayoutManager(this));
        binding.assignmentList.setAdapter(adapter);
        binding.btnAddAssignment.setOnClickListener(v -> openManageAssignment(0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        ArrayList<Assignment> assignments = database.getAssignmentsFromCourse(courseId);
        adapter.updateList(assignments);
        binding.emptyIndicator.setVisibility(assignments.isEmpty() ? View.VISIBLE : View.GONE);

        int[] progress = database.getCourseProgress(courseId);
        int percent = progress[1] == 0 ? 0 : progress[0] * 100 / progress[1];
        binding.progressText.setText(String.format(Locale.getDefault(),
                "%d of %d completed (%d%%)", progress[0], progress[1], percent));
        binding.progressBar.setProgress(percent);
    }

    private void openManageAssignment(int assignmentId) {
        Intent intent = new Intent(this, ManageAssignmentActivity.class);
        intent.putExtra(ManageAssignmentActivity.EXTRA_COURSE_ID, courseId);
        intent.putExtra(ManageAssignmentActivity.EXTRA_COURSE_NAME, courseName);
        intent.putExtra(ManageAssignmentActivity.EXTRA_ASSIGNMENT_ID, assignmentId);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
