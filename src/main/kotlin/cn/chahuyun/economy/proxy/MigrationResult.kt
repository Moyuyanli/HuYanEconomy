package cn.chahuyun.economy.data.proxy

/**
 * 杩佺Щ缁撴灉
 */
data class MigrationResult(
    val success: Boolean,
    val migratedCount: Int,
    val failedCount: Int,
    val errors: List<String>
) {
    companion object {
        fun success(count: Int) = MigrationResult(
            success = true,
            migratedCount = count,
            failedCount = 0,
            errors = emptyList()
        )

        fun failure(migrated: Int, failed: Int, errors: List<String>) = MigrationResult(
            success = false,
            migratedCount = migrated,
            failedCount = failed,
            errors = errors
        )
    }
}
