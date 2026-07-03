package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.UserStatusV1Converter
import cn.chahuyun.economy.converter.v2.UserStatusV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.entity.UserStatus
import cn.chahuyun.economy.entity.v2.user.UserStatusEntity
import cn.chahuyun.economy.model.user.UserStatusDto
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class UserStatusEntityProxy : EntityProxy<UserStatusDto> {

    private val v1Converter = UserStatusV1Converter()
    private val v2Converter = UserStatusV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): UserStatusDto? {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findV2ByUserId(id)
            else -> findV1ById(id)
        }
    }

    override fun findAll(): List<UserStatusDto> {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> findAllV2()
            else -> findAllV1()
        }
    }

    override fun findWhere(predicate: (UserStatusDto) -> Boolean): List<UserStatusDto> = findAll().filter(predicate)

    override fun save(dto: UserStatusDto): UserStatusDto {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> saveV2(dto)
            else -> saveV1(dto)
        }
    }

    override fun saveAll(dtos: List<UserStatusDto>): List<UserStatusDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean {
        return when (getCurrentVersion()) {
            DataVersion.V2 -> deleteV2ByUserId(id)
            else -> deleteV1(id)
        }
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("UserStatusEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): UserStatusDto? = try {
        HibernateFactory.selectOneById(UserStatus::class.java, id)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy query V1 status failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<UserStatusDto> = try {
        v1Converter.toDtoList(HibernateFactory.selectList(UserStatus::class.java))
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy query all V1 statuses failed", e)
        emptyList()
    }

    private fun saveV1(dto: UserStatusDto): UserStatusDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy save V1 status failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(UserStatus::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy delete V1 status failed: id=$id", e)
        false
    }

    private fun findV2ByUserId(userId: Long): UserStatusDto? = try {
        HibernateFactory.selectOne(UserStatusEntity::class.java, "userId", userId)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy query V2 status failed: userId=$userId", e)
        null
    }

    private fun findAllV2(): List<UserStatusDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(UserStatusEntity::class.java))
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy query all V2 statuses failed", e)
        emptyList()
    }

    private fun saveV2(dto: UserStatusDto): UserStatusDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = HibernateFactory.selectOne(UserStatusEntity::class.java, "userId", dto.id)
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy save V2 status failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2ByUserId(userId: Long): Boolean = try {
        val entity = HibernateFactory.selectOne(UserStatusEntity::class.java, "userId", userId)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("UserStatusEntityProxy delete V2 status failed: userId=$userId", e)
        false
    }

    private fun migrateV1ToV2(): MigrationResult {
        var migrated = 0
        var failed = 0
        val errors = mutableListOf<String>()
        findAllV1().forEach { dto ->
            try {
                saveV2(dto)
                migrated += 1
            } catch (e: Exception) {
                failed += 1
                errors += "id=${dto.id}: ${e.message ?: e::class.simpleName}"
            }
        }
        return if (failed == 0) MigrationResult.success(migrated) else MigrationResult.failure(migrated, failed, errors)
    }

    companion object {
        private const val MODULE_NAME = "user_status"
    }
}
