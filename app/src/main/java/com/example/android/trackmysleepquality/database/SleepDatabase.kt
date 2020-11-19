/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// add all the tables in the list (we only have SleepNight database)
@Database(entities = [SleepNight::class], version = 1, exportSchema = false)
// abstract - because Room creates the implementation for us
abstract class SleepDatabase : RoomDatabase() {

    // we can have multiple DAO's
    abstract val sleepDatabaseDao: SleepDatabaseDao;

    // we do not need to instantiate the class, so we have a companion object
    companion object {

        // @Volatile = it makes sure the value of INSTANCE is always up to date and
        // the same to all execution threads
        // the value will never be cached, and all writes and reads will be done to and from the main memory
        @Volatile
        // this will avoid us repeatedly opening connections to the database, which is expensive

        private var INSTANCE: SleepDatabase? = null

        // returns the reference to the SleepDatabase
        fun getInstance(context: Context): SleepDatabase {
            // synchronized = means only one thread of execution at a time can enter this
            // block of code
            synchronized(this) {
                // smart cast - available only to local variables (not class variables)
                var instance = INSTANCE;

                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.applicationContext,
                            SleepDatabase::class.java,
                            "sleep_history_database"
                    )
                            // normally, we need to provide a migration object with a migration
                            // strategy when we create the database
                            // MIGRATION STRATEGY = tells us how to take all the rows of the old
                            // schema and convert them to rows in the new schema
                            .fallbackToDestructiveMigration()
                            .build();
                    INSTANCE = instance
                }
                return instance;
            }
        }
    }
}