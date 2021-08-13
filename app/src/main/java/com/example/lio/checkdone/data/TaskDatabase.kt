package com.example.lio.checkdone.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lio.checkdone.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Task::class], version = 1)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    class Callback @Inject constructor(
        private val database: Provider<TaskDatabase>, //we get dependencies lazily
        @ApplicationScope private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            //db operations
            val dao = database.get().taskDao()

            applicationScope.launch {
                dao.insert(Task("Wash the dishes 1"))
                dao.insert(Task("Wash the dishes 2"))
                dao.insert(Task("Wash the dishes 3", important = true))
                dao.insert(Task("Wash the dishes 4", completed = true))
                dao.insert(Task("Wash the dishes 5"))
                dao.insert(Task("Wash the dishes 6"))
                dao.insert(Task("Wash the dishes 7", completed = true))
                dao.insert(Task("Wash the dishes 8"))
            }

        }
    }
}