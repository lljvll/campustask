package com.campustask.app

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campustask.app.data.CampusDatabase
import com.campustask.app.data.Course
import com.campustask.app.data.Task
import com.campustask.app.databinding.ActivityMainBinding
import com.campustask.app.databinding.ItemTaskBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class CampusViewModel : ViewModel() {
    var courses: List<Course> = emptyList()
    var tasks: List<Task> = emptyList()
    var screen: String = "dashboard"
    var filter: String = "All"
    var courseFilter: Int = -1
    var selectedTask: Int = -1
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: CampusViewModel
    private val worker = Executors.newSingleThreadExecutor()
    private val dao by lazy { CampusDatabase.get(this).dao() }
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val brand = Color.rgb(67, 85, 216)
    private val muted = Color.rgb(102, 113, 139)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this)[CampusViewModel::class.java]
        binding.navDashboard.setOnClickListener { show("dashboard") }
        binding.navTasks.setOnClickListener { show("tasks") }
        binding.navCourses.setOnClickListener { show("courses") }
        refresh()
    }

    private fun refresh(after: (() -> Unit)? = null) {
        worker.execute {
            val courses = dao.courses()
            val tasks = dao.tasks()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                vm.courses = courses
                vm.tasks = tasks
                if (after == null) render() else after()
            }
        }
    }

    private fun mutate(operation: () -> Unit, after: (() -> Unit)? = null) {
        worker.execute {
            try {
                operation()
                runOnUiThread { refresh(after) }
            } catch (error: Exception) {
                runOnUiThread { Toast.makeText(this, "Could not save. Check for duplicate course codes.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun show(screen: String) { vm.screen = screen; render() }
    private fun render() {
        binding.content.removeAllViews()
        binding.nav.visibility = if (vm.screen in listOf("dashboard", "tasks", "courses")) View.VISIBLE else View.GONE
        binding.pageTitle.text = when (vm.screen) {
            "dashboard" -> "Your dashboard"
            "tasks" -> "All tasks"
            "courses" -> "My courses"
            "editTask" -> if (vm.selectedTask == -1) "Add task" else "Edit task"
            "editCourse" -> if (vm.courseFilter == -1) "Add course" else "Edit course"
            "details" -> "Task details"
            else -> "CampusTask"
        }
        when (vm.screen) {
            "dashboard" -> dashboard()
            "tasks" -> taskList()
            "courses" -> coursesList()
            "editTask" -> taskForm()
            "editCourse" -> courseForm()
            "details" -> taskDetails()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }
    private fun scroll(content: View) {
        val sc = ScrollView(this).apply { fillViewport = true; addView(content) }
        binding.content.addView(sc)
    }
    private fun heading(text: String, size: Float = 20f) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(Color.rgb(24,33,59)); setTypeface(null,Typeface.BOLD)
        setPadding(0, dp(10), 0, dp(10))
    }
    private fun hint(text: String) = TextView(this).apply {
        this.text = text; textSize = 14f; setTextColor(muted); setPadding(0, dp(4), 0, dp(12))
    }
    private fun button(text: String, click: () -> Unit) = MaterialButton(this).apply {
        this.text = text
        setOnClickListener { click() }
    }
    private fun card(content: View): MaterialCardView = MaterialCardView(this).apply {
        radius = dp(15).toFloat()
        cardElevation = dp(1).toFloat()
        setCardBackgroundColor(Color.WHITE)
        addView(content)
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.bottomMargin = dp(12)
        layoutParams = params
    }
    private fun summary(title: String, value: Int): MaterialCardView {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(10), dp(12))
            addView(heading(value.toString(), 25f)); addView(hint(title))
        }
        return card(box)
    }
    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun dashboard() {
        val col = column()
        col.addView(heading("Stay on top of your semester", 22f))
        col.addView(hint("Your assignments, deadlines, and progress in one place."))
        val today = todayStart()
        val upcoming = vm.tasks.filter { !it.isCompleted && it.dueDate >= today }
        val overdue = vm.tasks.count { !it.isCompleted && it.dueDate < today }
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((label, count) in listOf("Upcoming" to upcoming.size, "Overdue" to overdue, "Done" to vm.tasks.count { it.isCompleted })) {
            val item = summary(label, count)
            item.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3),0,dp(3),0) }
            stats.addView(item)
        }
        col.addView(stats)
        col.addView(button("+ Add assignment") { vm.selectedTask = -1; show("editTask") })
        col.addView(heading("Next up"))
        if (upcoming.isEmpty()) col.addView(hint("No upcoming tasks. Add an assignment to get started."))
        upcoming.take(5).forEach { task -> col.addView(taskCard(task)) }
        col.addView(button("View all tasks") { show("tasks") })
        scroll(col)
    }

    private fun courseCode(id: Int) = vm.courses.find { it.courseId == id }?.courseCode ?: "Unknown course"
    private fun taskCard(task: Task): View {
        val row = ItemTaskBinding.inflate(layoutInflater)
        row.taskName.text = task.title
        row.taskMeta.text = "${courseCode(task.courseId)}  •  ${dateFormat.format(Date(task.dueDate))}"
        row.taskPriority.text = task.priority.uppercase()
        row.taskPriority.setTextColor(when (task.priority) { "High" -> Color.rgb(191,43,55); "Medium" -> Color.rgb(162,101,17); else -> brand })
        row.completed.setOnCheckedChangeListener(null)
        row.completed.isChecked = task.isCompleted
        row.completed.setOnCheckedChangeListener { _, checked -> mutate({ dao.updateTask(task.copy(isCompleted = checked)) }) }
        row.root.setOnClickListener { vm.selectedTask = task.taskId; show("details") }
        return card(row.root)
    }

    private fun taskList() {
        val col = column()
        col.addView(button("+ Add task") { vm.selectedTask = -1; show("editTask") })
        val statuses = listOf("All", "Active", "Completed")
        val statusSpinner = Spinner(this)
        statusSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
        statusSpinner.setSelection(statuses.indexOf(vm.filter))
        col.addView(heading("Status", 15f)); col.addView(statusSpinner)
        val courseOptions = listOf("All courses") + vm.courses.map { it.courseCode }
        val courseSpinner = Spinner(this)
        courseSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, courseOptions)
        courseSpinner.setSelection(vm.courses.indexOfFirst { it.courseId == vm.courseFilter }.let { if (it == -1) 0 else it + 1 })
        col.addView(heading("Course", 15f)); col.addView(courseSpinner)
        val count = hint("")
        col.addView(count)
        val list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@MainActivity); isNestedScrollingEnabled = false }
        col.addView(list)
        fun update() {
            val filtered = vm.tasks.filter { task ->
                (vm.filter == "All" || (vm.filter == "Active" && !task.isCompleted) || (vm.filter == "Completed" && task.isCompleted)) &&
                (vm.courseFilter == -1 || task.courseId == vm.courseFilter)
            }
            count.text = "${filtered.size} task(s)"
            list.adapter = TaskAdapter(filtered) { task -> vm.selectedTask = task.taskId; show("details") }
        }
        statusSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                vm.filter = statuses[position]; update()
            }
        }
        courseSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                vm.courseFilter = if (position == 0) -1 else vm.courses[position - 1].courseId; update()
            }
        }
        update()
        scroll(col)
    }

    private inner class TaskAdapter(val items: List<Task>, val onOpen: (Task) -> Unit) : RecyclerView.Adapter<TaskAdapter.Holder>() {
        inner class Holder(val ui: ItemTaskBinding) : RecyclerView.ViewHolder(ui.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(ItemTaskBinding.inflate(layoutInflater, parent, false))
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val task = items[position]
            holder.ui.taskName.text = task.title
            holder.ui.taskMeta.text = "${courseCode(task.courseId)}  •  ${dateFormat.format(Date(task.dueDate))}"
            holder.ui.taskPriority.text = task.priority.uppercase()
            holder.ui.taskPriority.setTextColor(when (task.priority) { "High" -> Color.RED; "Medium" -> Color.rgb(162,101,17); else -> brand })
            holder.ui.completed.setOnCheckedChangeListener(null)
            holder.ui.completed.isChecked = task.isCompleted
            holder.ui.completed.setOnCheckedChangeListener { _, checked -> mutate({ dao.updateTask(task.copy(isCompleted = checked)) }) }
            holder.ui.root.setOnClickListener { onOpen(task) }
        }
    }

    private fun coursesList() {
        val col = column()
        col.addView(button("+ Add course") { vm.courseFilter = -1; show("editCourse") })
        col.addView(heading("Your courses (${vm.courses.size})"))
        if (vm.courses.isEmpty()) col.addView(hint("No courses yet. Create a course before adding tasks."))
        vm.courses.forEach { course ->
            val box = column().apply {
                addView(heading(course.courseCode, 18f))
                addView(hint(course.courseName))
                addView(hint("${vm.tasks.count { it.courseId == course.courseId }} assignments"))
                val actions = LinearLayout(this@MainActivity)
                actions.addView(button("Edit") { vm.courseFilter = course.courseId; show("editCourse") })
                actions.addView(button("Delete") {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Delete ${course.courseCode}?")
                        .setMessage("All assignments in this course will also be deleted.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ -> mutate({ dao.deleteCourse(course) }) { show("courses") } }.show()
                })
                addView(actions)
            }
            col.addView(card(box))
        }
        scroll(col)
    }

    private fun field(label: String, initial: String = "", multiline: Boolean = false): EditText = EditText(this).apply {
        hint = label; setText(initial); setSingleLine(!multiline)
        if (multiline) minLines = 3
        textSize = 16f
    }
    private fun courseForm() {
        val existing = vm.courses.find { it.courseId == vm.courseFilter }
        val col = column()
        col.addView(button("← Back to courses") { show("courses") })
        val code = field("Course code (e.g. COMP3074)", existing?.courseCode ?: "")
        val name = field("Course name", existing?.courseName ?: "")
        col.addView(heading("Course code")); col.addView(code)
        col.addView(heading("Course name")); col.addView(name)
        col.addView(button("Save course") {
            val c = code.text.toString().trim().uppercase(Locale.getDefault())
            val n = name.text.toString().trim()
            if (c.isEmpty()) { code.error = "Course code is required"; return@button }
            if (n.isEmpty()) { name.error = "Course name is required"; return@button }
            mutate({ if (existing == null) dao.insertCourse(Course(courseCode = c, courseName = n))
                      else dao.updateCourse(existing.copy(courseCode = c, courseName = n)) }) { show("courses") }
        })
        scroll(col)
    }

    private fun taskForm() {
        if (vm.courses.isEmpty()) {
            val col = column()
            col.addView(heading("Create a course first"))
            col.addView(hint("Every assignment must belong to a course."))
            col.addView(button("Add course") { vm.courseFilter = -1; show("editCourse") })
            col.addView(button("← Back") { show("tasks") })
            scroll(col); return
        }
        val existing = vm.tasks.find { it.taskId == vm.selectedTask }
        val col = column()
        col.addView(button("← Back to tasks") { show("tasks") })
        val title = field("Assignment title", existing?.title ?: "")
        col.addView(heading("Title", 16f)); col.addView(title)
        col.addView(heading("Course", 16f))
        val courses = Spinner(this)
        courses.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vm.courses.map { "${it.courseCode} — ${it.courseName}" })
        courses.setSelection(vm.courses.indexOfFirst { it.courseId == existing?.courseId }.coerceAtLeast(0))
        col.addView(courses)
        col.addView(heading("Due date", 16f))
        var due = existing?.dueDate ?: todayStart()
        lateinit var dueButton: MaterialButton
        dueButton = button(dateFormat.format(Date(due))) {
            val date = Calendar.getInstance().apply { timeInMillis = due }
            DatePickerDialog(this, { _, year, month, day ->
                due = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                // The button text is updated via the captured reference below.
                dueButton.text = dateFormat.format(Date(due))
            }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
        }
        col.addView(dueButton)
        col.addView(heading("Priority", 16f))
        val priorities = listOf("Low", "Medium", "High")
        val priority = Spinner(this)
        priority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)
        priority.setSelection(priorities.indexOf(existing?.priority ?: "Medium"))
        col.addView(priority)
        col.addView(heading("Notes (optional)", 16f))
        val notes = field("Notes", existing?.notes ?: "", true)
        col.addView(notes)
        col.addView(button("Save task") {
            val text = title.text.toString().trim()
            if (text.isEmpty()) { title.error = "Title is required"; return@button }
            val task = Task(taskId = existing?.taskId ?: 0, title = text,
                courseId = vm.courses[courses.selectedItemPosition].courseId,
                dueDate = due, priority = priorities[priority.selectedItemPosition],
                notes = notes.text.toString().trim(), isCompleted = existing?.isCompleted ?: false)
            mutate({ if (existing == null) dao.insertTask(task) else dao.updateTask(task) }) { show("tasks") }
        })
        scroll(col)
    }

    private fun taskDetails() {
        val task = vm.tasks.find { it.taskId == vm.selectedTask } ?: run { show("tasks"); return }
        val col = column()
        col.addView(button("← All tasks") { show("tasks") })
        col.addView(heading(task.title, 26f))
        col.addView(hint("Course: ${courseCode(task.courseId)}"))
        col.addView(hint("Due: ${dateFormat.format(Date(task.dueDate))}"))
        col.addView(hint("Priority: ${task.priority}"))
        col.addView(hint("Status: ${if (task.isCompleted) "Completed" else "Active"}"))
        col.addView(heading("Notes", 17f))
        col.addView(hint(task.notes.ifBlank { "No notes added" }))
        col.addView(button(if (task.isCompleted) "Mark incomplete" else "Mark complete") {
            mutate({ dao.updateTask(task.copy(isCompleted = !task.isCompleted)) }) { show("details") }
        })
        col.addView(button("Edit task") { show("editTask") })
        col.addView(button("Delete task") {
            AlertDialog.Builder(this).setTitle("Delete assignment?")
                .setMessage("This cannot be undone.").setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ -> mutate({ dao.deleteTask(task) }) { show("tasks") } }.show()
        })
        scroll(col)
    }

    override fun onBackPressed() {
        when (vm.screen) { "dashboard" -> super.onBackPressed(); "details", "editTask" -> show("tasks"); "editCourse" -> show("courses"); else -> show("dashboard") }
    }
    override fun onDestroy() { worker.shutdown(); super.onDestroy() }
}
