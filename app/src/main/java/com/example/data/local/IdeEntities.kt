package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.BuildStatus
import com.example.data.model.IdeFileType
import com.example.data.model.ProjectTemplate

@Entity(tableName = "ide_projects")
data class IdeProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageName: String,
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val template: ProjectTemplate = ProjectTemplate.EMPTY_COMPOSE,
    val lastBuildStatus: BuildStatus = BuildStatus.IDLE,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "ide_files",
    foreignKeys = [
        ForeignKey(
            entity = IdeProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["projectId"])],
)
data class IdeFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val folderPath: String,
    val name: String,
    val fileType: IdeFileType,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val fullPath: String
        get() = if (folderPath.isBlank()) name else "$folderPath/$name"
}

data class ProjectWithStats(
    val project: IdeProjectEntity,
    val fileCount: Int,
)
