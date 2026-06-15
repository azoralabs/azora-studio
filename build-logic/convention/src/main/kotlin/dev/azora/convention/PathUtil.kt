package dev.azora.convention

import org.gradle.api.Project
import java.util.Locale

/**
 * Converts the project path to a fully qualified package name.
 *
 * Transforms the Gradle project path (e.g., `:core:data`) into a Java package name
 * by replacing colons with dots and prepending the base package.
 *
 * ## Examples
 * - `:core:data` → `dev.azora.core.data`
 *
 * @return The fully qualified package name
 */
fun Project.pathToPackageName(): String {
    val relativePackageName = path
        .replace(":", ".")
        .replace("-", "")
        .lowercase()

    return "dev.azora$relativePackageName"
}

/**
 * Converts the project path to an Android resource prefix.
 *
 * Creates a unique prefix for Android resources to avoid conflicts between modules.
 * Replaces colons with underscores and appends an underscore.
 *
 * ## Examples
 * - `:core:data` → `core_data_`
 *
 * @return The resource prefix string
 */
fun Project.pathToResourcePrefix() = path
    .replace(":", "_")
    .replace("-", "_")
    .lowercase()
    .drop(1) + "_"

/**
 * Converts the project path to an iOS framework name.
 *
 * Creates a PascalCase name suitable for iOS frameworks by capitalizing
 * each path segment.
 *
 * ## Examples
 * - `:core:data` → `CoreData`
 *
 * @return The framework name in PascalCase
 */
fun Project.pathToFrameworkName(): String {
    val parts = this.path.split(":", "-", "_", " ")
    return parts.joinToString { part ->
        part.replaceFirstChar {
            it.titlecase(Locale.ROOT)
        }
    }
}