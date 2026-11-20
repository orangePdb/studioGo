package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BuildStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeDao {

    @Query("SELECT * FROM ide_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<IdeProjectEntity>>

    @Query("SELECT COUNT(*) FROM ide_projects")
    suspend fun getProjectCount(): Int

    @Query("SELECT * FROM ide_projects WHERE id = :projectId")
    fun observeProject(projectId: Long): Flow<IdeProjectEntity?>

    @Query("SELECT * FROM ide_projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): IdeProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: IdeProjectEntity): Long

    @Update
    suspend fun updateProject(project: IdeProjectEntity)

    @Query("UPDATE ide_projects SET lastBuildStatus = :status, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectBuildStatus(projectId: Long, status: BuildStatus, updatedAt: Long)

    @Query("DELETE FROM ide_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Query("SELECT * FROM ide_files WHERE projectId = :projectId ORDER BY folderPath ASC, name ASC")
    fun observeFilesForProject(projectId: Long): Flow<List<IdeFileEntity>>

    @Query("SELECT * FROM ide_files")
    fun observeAllFiles(): Flow<List<IdeFileEntity>>

    @Query("SELECT * FROM ide_files WHERE projectId = :projectId")
    suspend fun getFilesForProjectOnce(projectId: Long): List<IdeFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: IdeFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<IdeFileEntity>): List<Long>

    @Query("UPDATE ide_files SET content = :content, updatedAt = :updatedAt WHERE id = :fileId")
    suspend fun updateFileContent(fileId: Long, content: String, updatedAt: Long)

    @Query("DELETE FROM ide_files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: Long)
}
