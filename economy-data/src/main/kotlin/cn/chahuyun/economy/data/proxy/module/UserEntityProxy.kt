package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.UserInfoV1Converter
import cn.chahuyun.economy.converter.v2.UserInfoV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.data.repository.UserInfoRepository
import cn.chahuyun.economy.entity.v2.user.UserEntity
import cn.chahuyun.economy.model.user.UserBackpackDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.utils.Log

/**
 * User data proxy.
 *
 * Business code talks to UserInfoDto only; this proxy chooses the entity version.
 */
class UserEntityProxy : EntityProxy<UserInfoDto> {

    private val v1Converter = UserInfoV1Converter()
    private val v2Converter = UserInfoV2Converter()
    private val backpackProxy = UserBackpackEntityProxy()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): UserInfoDto? {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findV2ById(id) ?: findV2ByQq(id)
            else -> findV1ById(id)
        }
    }

    override fun findByKey(key: String): UserInfoDto? {
        val qq = key.toLongOrNull() ?: return null
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findV2ByQq(qq)
            else -> findV1ByQq(qq)
        }
    }

    override fun findAll(): List<UserInfoDto> {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findAllV2()
            else -> findAllV1()
        }
    }

    override fun findWhere(predicate: (UserInfoDto) -> Boolean): List<UserInfoDto> {
        return findAll().filter(predicate)
    }

    override fun save(dto: UserInfoDto): UserInfoDto {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> saveV2(dto)
            else -> saveV1(dto)
        }
    }

    override fun saveAll(dtos: List<UserInfoDto>): List<UserInfoDto> {
        return dtos.map { save(it) }
    }

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
                errors = listOf("UserEntityProxy does not support migration to $targetVersion yet")
            )
        }
    }

    private fun findV1ById(id: Long): UserInfoDto? {
        return try {
            val entity = UserInfoRepository.findById(id.toString())
            entity?.let { v1Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("UserEntityProxy query V1 user failed: id=$id", e)
            null
        }
    }

    private fun findV1ByQq(qq: Long): UserInfoDto? {
        return try {
            val entity = UserInfoRepository.findByQq(qq)
            entity?.let { v1Converter.toDto(it) }
        } catch (e: Exception) {
            Log.error("UserEntityProxy query V1 user failed: qq=$qq", e)
            null
        }
    }

    private fun findAllV1(): List<UserInfoDto> {
        return try {
            val entities = UserInfoRepository.listAll()
            v1Converter.toDtoList(entities)
        } catch (e: Exception) {
            Log.error("UserEntityProxy query all V1 users failed", e)
            emptyList()
        }
    }

    private fun saveV1(dto: UserInfoDto): UserInfoDto {
        return try {
            val entity = v1Converter.toEntity(dto)
            val saved = UserInfoRepository.save(entity)
            v1Converter.toDto(saved)
        } catch (e: Exception) {
            Log.error("UserEntityProxy save V1 user failed: qq=${dto.qq}", e)
            dto
        }
    }

    private fun deleteV1(id: Long): Boolean {
        return try {
            UserInfoRepository.deleteById(id.toString())
        } catch (e: Exception) {
            Log.error("UserEntityProxy delete V1 user failed: id=$id", e)
            false
        }
    }

    private fun findV2ById(id: Long): UserInfoDto? {
        return try {
            val entity = UserInfoRepository.findV2ById(id)
            entity?.let { withBackpacks(v2Converter.toDto(it)) }
        } catch (e: Exception) {
            Log.error("UserEntityProxy query V2 user failed: id=$id", e)
            null
        }
    }

    private fun findV2ByQq(qq: Long): UserInfoDto? {
        return try {
            val entity = UserInfoRepository.findV2ByQq(qq)
            entity?.let { withBackpacks(v2Converter.toDto(it)) }
        } catch (e: Exception) {
            Log.error("UserEntityProxy query V2 user failed: qq=$qq", e)
            null
        }
    }

    private fun findV2ByUserKey(userKey: String): UserEntity? {
        if (userKey.isEmpty()) return null
        return try {
            UserInfoRepository.findV2ByUserKey(userKey)
        } catch (e: Exception) {
            Log.error("UserEntityProxy query V2 user failed: userKey=$userKey", e)
            null
        }
    }

    private fun findAllV2(): List<UserInfoDto> {
        return try {
            val entities = UserInfoRepository.listAllV2()
            v2Converter.toDtoList(entities).map { withBackpacks(it) }
        } catch (e: Exception) {
            Log.error("UserEntityProxy query all V2 users failed", e)
            emptyList()
        }
    }

    private fun saveV2(dto: UserInfoDto): UserInfoDto {
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

            val saved = UserInfoRepository.saveV2(entity)
            val savedDto = v2Converter.toDto(saved)
            saveBackpacks(dto.backpacks, savedDto.id)
            withBackpacks(savedDto)
        } catch (e: Exception) {
            Log.error("UserEntityProxy save V2 user failed: qq=${dto.qq}", e)
            dto
        }
    }

    private fun deleteV2(id: Long): Boolean {
        return try {
            UserInfoRepository.deleteV2ByIdOrQq(id)
        } catch (e: Exception) {
            Log.error("UserEntityProxy delete V2 user failed: id=$id", e)
            false
        }
    }

    private fun findExistingV2Entity(dto: UserInfoDto): UserEntity? {
        return findV2ByUserKey(dto.id)
            ?: runCatching { UserInfoRepository.findV2ByQq(dto.qq) }
                .onFailure { Log.error("UserEntityProxy query V2 user failed: qq=${dto.qq}", it) }
                .getOrNull()
    }

    private fun migrateV1ToV2(): MigrationResult {
        val errors = mutableListOf<String>()
        var migrated = 0
        var failed = 0

        Log.info("UserEntityProxy starts migrating V1 users to V2")
        findAllV1().forEach { dto ->
            try {
                saveV2(dto)
                saveBackpacks(dto.backpacks, dto.id)
                migrated += 1
            } catch (e: Exception) {
                failed += 1
                errors += "qq=${dto.qq}: ${e.message ?: e::class.simpleName}"
                Log.error("UserEntityProxy migrate V1 user to V2 failed: qq=${dto.qq}", e)
            }
        }

        return if (failed == 0) {
            MigrationResult.success(migrated)
        } else {
            MigrationResult.failure(migrated, failed, errors)
        }
    }

    private fun withBackpacks(dto: UserInfoDto): UserInfoDto {
        val backpacks = findBackpacks(dto.id)
        dto.backpacks = backpacks
        dto.backpackCount = backpacks.size
        return dto
    }

    private fun findBackpacks(userKey: String): List<UserBackpackDto> {
        if (userKey.isEmpty()) return emptyList()
        return backpackProxy.findWhere { it.userId == userKey }
    }

    private fun saveBackpacks(backpacks: List<UserBackpackDto>, userKey: String) {
        if (backpacks.isEmpty() || userKey.isEmpty()) return
        backpackProxy.saveAll(backpacks.map { it.copy(userId = userKey) })
    }

    companion object {
        private const val MODULE_NAME = "user"
    }
}
