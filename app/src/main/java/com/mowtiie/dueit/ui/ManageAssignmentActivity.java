package com.mowtiie.dueit.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mowtiie.dueit.data.Assignment;
import com.mowtiie.dueit.data.DueItDatabase;
import com.mowtiie.dueit.databinding.ActivityManageAssignmentBinding;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class ManageAssignmentActivity extends AppCompatActivity {

    public static final String EXTRA_COURSE_ID = "EXTRA_COURSE_ID";
    public static final String EXTRA_COURSE_NAME = "EXTRA_COURSE_NAME";
    public static final String EXTRA_ASSIGNMENT_ID = "EXTRA_ASSIGNMENT_ID";

    private static final String[] PRIORITIES = {"Low", "Medium", "High"};

    ActivityManageAssignmentBinding binding;
    DueItDatabase database;
    int courseId;
    int assignmentId;
    String courseName;
    String status = DueItDatabase.STATUS_PENDING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityManageAssignmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = new DueItDatabase(this);
        courseId = getIntent().getIntExtra(EXTRA_COURSE_ID, -1);
        courseName = getIntent().getStringExtra(EXTRA_COURSE_NAME);
        assignmentId = getIntent().getIntExtra(EXTRA_ASSIGNMENT_ID, 0);
        boolean editing = assignmentId != 0;

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(editing ? "Edit assignment" : "Add assignment");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.priorityInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, PRIORITIES));
        binding.priorityInput.setText("Medium", false);
        binding.dueDateInput.setOnClickListener(v -> pickDateTime());

        if (editing) {
            Assignment assignment = database.getAssignment(assignmentId);
            if (assignment == null) {
                finish();
                return;
            }
            status = assignment.status;
            binding.titleInput.setText(assignment.title);
            binding.dueDateInput.setText(assignment.dueDate);
            binding.priorityInput.setText(assignment.priorityLevel, false);
            binding.descriptionInput.setText(assignment.description);
            binding.btnDelete.setVisibility(View.VISIBLE);
        }

        binding.btnSave.setOnClickListener(v -> save(editing));
        binding.btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle("Delete assignment?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    database.deleteAssignment(assignmentId);
                    finish();
                })
                .show());
    }

    private void pickDateTime() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                new TimePickerDialog(this, (timeView, hour, minute) ->
                        binding.dueDateInput.setText(String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                                year, month + 1, day, hour, minute)),
                        now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void save(boolean editing) {
        String title = Objects.requireNonNull(binding.titleInput.getText()).toString().trim();
        String dueDate = Objects.requireNonNull(binding.dueDateInput.getText()).toString().trim();
        String priority = binding.priorityInput.getText().toString().trim();
        String description = Objects.requireNonNull(binding.descriptionInput.getText()).toString().trim();

        if (title.isEmpty() || dueDate.isEmpty() || priority.isEmpty()) {
            Toast.makeText(this, "Title, due date and priority are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved = editing
                ? database.updateAssignment(assignmentId, title, dueDate, description, priority)
                : database.addAssignment(courseId, title, dueDate, description, priority);
        if (!saved) {
            Toast.makeText(this, "Could not save the assignment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (priority.equals("High") && status.equals(DueItDatabase.STATUS_PENDING)) {
            // New rows have no id yet in this screen, so fall back to a time-based id for the notification.
            int notificationId = editing ? assignmentId : (int) (System.currentTimeMillis() & 0x7fffffff);
            NotificationHelper.notifyHighPriority(this, notificationId, title, courseName != null ? courseName : "", dueDate);
        }
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
