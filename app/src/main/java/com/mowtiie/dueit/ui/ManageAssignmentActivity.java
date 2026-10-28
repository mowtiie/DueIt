package com.mowtiie.dueit.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.mowtiie.dueit.data.Assignment;
import com.mowtiie.dueit.data.DueItDatabase;
import com.mowtiie.dueit.databinding.ActivityManageAssignmentBinding;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

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
    String originalDueDate = "";

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
            originalDueDate = assignment.dueDate;
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

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
    }

    private void pickDateTime() {
        long today = MaterialDatePicker.todayInUtcMilliseconds();
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(today)
                .setValidator(DateValidatorPointForward.from(today))
                .build();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select due date")
                .setCalendarConstraints(constraints)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar picked = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            picked.setTimeInMillis(selection);
            String date = String.format(Locale.US, "%04d-%02d-%02d",
                    picked.get(Calendar.YEAR), picked.get(Calendar.MONTH) + 1, picked.get(Calendar.DAY_OF_MONTH));

            Calendar current = Calendar.getInstance();
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(current.get(Calendar.HOUR_OF_DAY))
                    .setMinute(current.get(Calendar.MINUTE))
                    .setTitleText("Select due time")
                    .build();
            timePicker.addOnPositiveButtonClickListener(v -> {
                String dueDate = String.format(Locale.US, "%s %02d:%02d", date, timePicker.getHour(), timePicker.getMinute());
                if (dueDate.compareTo(now()) < 0) {
                    Toast.makeText(this, "Due date and time can't be in the past!", Toast.LENGTH_SHORT).show();
                } else {
                    binding.dueDateInput.setText(dueDate);
                }
            });
            timePicker.show(getSupportFragmentManager(), "due_time");
        });
        datePicker.show(getSupportFragmentManager(), "due_date");
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

        if (title.length() > 100 || description.length() > 500) {
            Toast.makeText(this, "Title must be 100 characters or less and description 500 or less!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Arrays.asList(PRIORITIES).contains(priority)) {
            Toast.makeText(this, "Choose Low, Medium or High priority!", Toast.LENGTH_SHORT).show();
            return;
        }

        // An already-overdue assignment can still be edited as long as its due date is untouched.
        if (!dueDate.equals(originalDueDate) && dueDate.compareTo(now()) < 0) {
            Toast.makeText(this, "Due date and time can't be in the past!", Toast.LENGTH_SHORT).show();
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
