package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.UserBackpackV1Converter
import cn.chahuyun.economy.converter.v2.UserBackpackV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.data.repository.UserBackpackRepository
import cn.chahuyun.economy.entity.v2.user.UserBackpackEntity
import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.utils.Log

/**
 * User backpack data proxy.
 */
class UserBackpackEntityProxy : EntityProxy<UserBackpackDto> {

    private val v1Converter = UserBackpackV1Converter()
    private val v2Converter = UserBackpackV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): UserBackpackDto? {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findV2ById(id)
            else -> findV1ById(id)
        }
    }

    override fun findByKey(key: String): UserBackpackDto? {
        return findWhere { it.userId == key }.firstOrNull()
    }

    override fun findAll(): List<UserBackpackDto> {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findAllV2()
            else -> findAllV1()
        }
    }

    override fun findWhere(predicate: (UserBackpackDto) -> Boolean): List<UserBackpackDto> = findAll().filter(predicate)

    override fun save(dto: UserBackpackDto): UserBackpackDto {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> saveV2(dto)
            else -> saveV1(dto)
        }
    }

    override fun saveAll(dtos: List<UserBackpackDto>): List<UserBackpackDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> deleteV2(id)
            else -> deleteV1(id)
        }
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(
                migrated = 0,
                failed = 0,
                errors = listOf("UserBackpackEntityProxy does not support migration to $targetVersion yet")
            )
        }
    }

    private fun findV1ById(id: Long): UserBackpackDto? {
        return try {
            val entity = UserBackpackRepository.findById(id)
            entity?.let { v1Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy query V1 backpack failed: id=$id", e)
            null
        }
    }

    private fun findAllV1(): List<UserBackpackDto> {
        return try {
            v1Converter.toDtoList(UserBackpackRepository.listAll())
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy query all V1 backpacks failed", e)
            emptyList()
        }
    }

    private fun saveV1(dto: UserBackpackDto): UserBackpackDto {
        return try {
            v1Converter.toDto(UserBackpackRepository.save(v1Converter.toEntity(dto)))
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy save V1 backpack failed: id=${dto.id}", e)
            dto
        }
    }

    private fun deleteV1(id: Long): Boolean {
        return try {
            UserBackpackRepository.deleteById(id)
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy delete V1 backpack failed: id=$id", e)
            false
        }
    }

    private fun findV2ById(id: Long): UserBackpackDto? {
        return try {
            val entity = UserBackpackRepository.findV2ById(id)
            entity?.let { v2Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy query V2 backpack failed: id=$id", e)
            null
        }
    }

    private fun findAllV2(): List<UserBackpackDto> {
        return try {
            v2Converter.toDtoList(UserBackpackRepository.listAllV2())
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy query all V2 backpacks failed", e)
            emptyList()
        }
    }

    private fun saveV2(dto: UserBackpackDto): UserBackpackDto {
        return try {
            val entity = v2Converter.toEntity(dto)
            val existing = findExistingV2Entity(dto)
            val now = System.currentTimeMillis()

            if (existing != null) {
                entity.id = existing.id
                entity.createdAt = existing.createdAt
            }
            if (entity.createdAt == 0L) {
                entity.createdAt = now
            }
            entity.updatedAt = now

            v2Converter.toDto(UserBackpackRepository.saveV2(entity))
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy save V2 backpack failed: id=${dto.id}", e)
            dto
        }
    }

    private fun deleteV2(id: Long): Boolean {
        return try {
            UserBackpackRepository.deleteV2ById(id)
        } catch (e: Exception) {
            Log.error("UserBackpackEntityProxy delete V2 backpack failed: id=$id", e)
            false
        }
    }

    private fun findExistingV2Entity(dto: UserBackpackDto): UserBackpackEntity? {
        if (dto.id != 0L) {
            return runCatching { UserBackpackRepository.findV2ById(dto.id) }
                .onFailure { Log.error("UserBackpackEntityProxy query V2 backpack failed: id=${dto.id}", it) }
                .getOrNull()
        }

        return runCatching {
            UserBackpackRepository.findV2ByIdentity(dto.userId, dto.propCode, dto.propKind, dto.propId)
        }.onFailure {
            Log.error("UserBackpackEntityProxy query V2 backpack failed: propId=${dto.propId}", it)
        }.getOrNull()
    }

    private fun migrateV1ToV2(): MigrationResult {
        val errors = mutableListOf<String>()
        var migrated = 0
        var failed = 0

        Log.info("UserBackpackEntityProxy starts migrating V1 backpacks to V2")
        findAllV1().forEach { dto ->
            try {
                saveV2(dto)
                migrated += 1
            } catch (e: Exception) {
                failed += 1
                errors += "id=${dto.id}: ${e.message ?: e::class.simpleName}"
                Log.error("UserBackpackEntityProxy migrate V1 backpack to V2 failed: id=${dto.id}", e)
            }
        }

        return if (failed == 0) {
            MigrationResult.success(migrated)
        } else {
            MigrationResult.failure(migrated, failed, errors)
        }
    }

    companion object {
        private const val MODULE_NAME = "user_backpack"
    }
}
