package dev.azora.sdk.core.data.database

import androidx.sqlite.SQLiteException
import dev.azora.sdk.core.domain.util.*

/**
 * Executes a database update operation safely and maps SQLite errors to domain results.
 *
 * This helper function wraps a suspending database update call in a `try-catch` block
 * and converts low-level [SQLiteException]s into a domain-level [dev.azora.sdk.core.domain.util.Res].
 *
 * Behavior:
 * - On success, returns [dev.azora.sdk.core.domain.util.Res.Success] containing the update result
 * - On [SQLiteException], returns [dev.azora.sdk.core.domain.util.Res.Failure] with [dev.azora.sdk.core.domain.util.DataError.Local.DISK_FULL]
 *
 * This function is intended for write/update operations where disk-related failures
 * must be handled gracefully and surfaced in a consistent, domain-friendly way.
 *
 * @param update Suspending lambda that performs the database update
 * @return [dev.azora.sdk.core.domain.util.Res.Success] if the update succeeds, or [dev.azora.sdk.core.domain.util.Res.Failure] if a disk-related
 *         SQLite error occurs
 *
 * @see dev.azora.sdk.core.domain.util.Res
 * @see dev.azora.sdk.core.domain.util.DataError.Local
 */
suspend inline fun <T> safeDatabaseUpdate(
    update: suspend () -> T
) = try {
    Res.Success(update())
} catch (_: SQLiteException) {
    Res.Failure(DataError.Local.DISK_FULL)
}