package dev.azora.local.database.entity.project

import androidx.room.*

@Entity(tableName = "azora_projects")
data class AzoraProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val companyName: String,
    val packageName: String,
    val version: String,
    val engineVersion: String,
    val createdAt: Long,
    val updatedAt: Long?
)
