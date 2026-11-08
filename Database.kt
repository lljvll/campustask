package com.campustask.app.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "courses", indices = [Index(value = ["courseCode"], unique = true)])
data class Course(
    @PrimaryKey(autoGenerate = true) val courseId: Int = 0,
    val courseCode: String,
    val courseName: String
)

@Entity(tableName = "tasks", foreignKeys = [ForeignKey(entity = Course::class,
    parentColumns = ["courseId"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["courseId"])])
data class Task(
    @PrimaryKey(autoGenerate = true) val taskId: Int = 0,
    val title: String,
    val courseId: Int,
    val dueDate: Long,
    val priority: String,
    val notes: String = "",
    val isCompleted: Boolean = false
)

@Dao
interface CampusDao {
    @Query("SELECT * FROM courses ORDER BY courseCode") fun courses(): List<Course>
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDate ASC, CASE priority WHEN 'High' THEN 0 WHEN 'Medium' THEN 1 ELSE 2 END") fun tasks(): List<Task>
    @Insert(onConflict = OnConflictStrategy.ABORT) fun insertCourse(course: Course)
    @Update fun updateCourse(course: Course)
    @Delete fun deleteCourse(course: Course)
    @Insert fun insertTask(task: Task)
    @Update fun updateTask(task: Task)
    @Delete fun deleteTask(task: Task)
}

@Database(entities = [Course::class, Task::class], version = 1, exportSchema = false)
abstract class CampusDatabase : RoomDatabase() {
    abstract fun dao(): CampusDao
    companion object {
        @Volatile private var instance: CampusDatabase? = null
        fun get(context: Context): CampusDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,
                CampusDatabase::class.java, "campustask.db").build().also { instance = it }
        }
    }
}
