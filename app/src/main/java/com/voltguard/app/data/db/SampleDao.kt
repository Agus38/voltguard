package com.voltguard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: SampleEntity)

    @Query("SELECT * FROM samples ORDER BY ts DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SampleEntity>

    @Query("SELECT MAX(ts) FROM samples")
    suspend fun latestTs(): Long?

    @Query("DELETE FROM samples WHERE ts < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM samples")
    suspend fun count(): Int

    @Query("DELETE FROM samples")
    suspend fun clear()
}
