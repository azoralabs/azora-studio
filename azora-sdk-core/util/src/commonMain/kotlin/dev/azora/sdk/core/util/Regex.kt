package dev.azora.sdk.core.util

/**
 * Regex pattern for validating international phone numbers in E.164 format.
 *
 * Matches phone numbers that:
 * - Optionally start with a '+' sign
 * - Begin with a digit from 1-9
 * - Contain 1 to 14 additional digits (total 2-15 digits)
 *
 * Examples: "+12025551234", "442071234567", "33123456789"
 */
val phoneRegex = Regex("""^\+?[1-9]\d{1,14}$""")

/**
 * Regex pattern for validating strong passwords.
 *
 * Ensures the password:
 * - Contains at least one lowercase letter
 * - Contains at least one uppercase letter
 * - Contains at least one digit
 * - Contains at least one special character (non-alphanumeric)
 * - Has a length between 8 and 32 characters
 */
val strongPasswordRegex = Regex("""^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{8,32}$""")

/**
 * Regex pattern for validating email addresses.
 *
 * Matches standard email formats with:
 * - Local part: alphanumeric characters and ._%+-
 * - @ symbol
 * - Domain: alphanumeric characters and dots
 * - TLD: at least 2 alphabetic characters
 *
 * Case-insensitive matching is enabled.
 *
 * Examples: "user@example.com", "test.user+tag@sub.domain.org"
 */
val emailRegex = Regex("""^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}$""", RegexOption.IGNORE_CASE)

/**
 * Regex pattern for validating personal names.
 *
 * Matches names containing:
 * - Unicode letters (\p{L})
 * - Unicode combining marks (\p{M})
 * - Spaces, hyphens, and apostrophes
 *
 * Supports international names with diacritics and various naming conventions.
 *
 * Examples: "María García", "Jean-Pierre", "O'Connor", "Müller"
 */
val nameRegex = Regex("""^[\p{L}\p{M}\s\-']+$""")