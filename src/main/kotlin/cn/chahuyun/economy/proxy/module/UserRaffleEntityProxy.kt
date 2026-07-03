package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.UserRaffleV1Converter
import cn.chahuyun.economy.converter.v2.UserRaffleV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.entity.UserRaffle
import cn.chahuyun.economy.entity.v2.user.UserRaffleEntity
import cn.chahuyun.economy.model.user.UserRaffleDto
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.hibernateplus.HibernateFactory

class UserRaffleEntityProxy : EntityProxy<UserRaffleDto> {

    private val v1Converter = UserRaffleV1Converter()
    private val v2Converter = UserRaffleV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): UserRaffleDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ByUserId(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<UserRaffleDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (UserRaffleDto) -> Boolean): List<UserRaffleDto> = findAll().filter(predicate)

    override fun save(dto: UserRaffleDto): UserRaffleDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<UserRaffleDto>): List<UserRaffleDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2ByUserId(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("UserRaffleEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): UserRaffleDto? = try {
        val entity = HibernateFactory.getSessionFactory().fromTransaction { session ->
            session.find(UserRaffle::class.java, id)?.also { it.poolTimes.size }
        }
        entity?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy query V1 raffle user failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<UserRaffleDto> = try {
        val entities = HibernateFactory.getSessionFactory().fromTransaction { session ->
            session.createQuery("from UserRaffle", UserRaffle::class.java).resultList
                .onEach { it.poolTimes.size }
        }
        v1Converter.toDtoList(entities)
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy query all V1 raffle users failed", e)
        emptyList()
    }

    private fun saveV1(dto: UserRaffleDto): UserRaffleDto = try {
        v1Converter.toDto(HibernateFactory.merge(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy save V1 raffle user failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        val entity = HibernateFactory.selectOneById(UserRaffle::class.java, id)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy delete V1 raffle user failed: id=$id", e)
        false
    }

    private fun findV2ByUserId(userId: Long): UserRaffleDto? = try {
        HibernateFactory.selectOne(UserRaffleEntity::class.java, "userId", userId)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy query V2 raffle user failed: userId=$userId", e)
        null
    }

    private fun findAllV2(): List<UserRaffleDto> = try {
        v2Converter.toDtoList(HibernateFactory.selectList(UserRaffleEntity::class.java))
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy query all V2 raffle users failed", e)
        emptyList()
    }

    private fun saveV2(dto: UserRaffleDto): UserRaffleDto = try {
        val entity = v2Converter.toEntity(dto)
        val existing = HibernateFactory.selectOne(UserRaffleEntity::class.java, "userId", dto.id)
        val now = System.currentTimeMillis()
        if (existing != null) {
            entity.id = existing.id
            entity.createdAt = existing.createdAt
        }
        if (entity.createdAt == 0L) entity.createdAt = now
        entity.updatedAt = now
        v2Converter.toDto(HibernateFactory.merge(entity))
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy save V2 raffle user failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2ByUserId(userId: Long): Boolean = try {
        val entity = HibernateFactory.selectOne(UserRaffleEntity::class.java, "userId", userId)
        if (entity == null) false else {
            HibernateFactory.delete(entity)
            true
        }
    } catch (e: Exception) {
        Log.error("UserRaffleEntityProxy delete V2 raffle user failed: userId=$userId", e)
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
        private const val MODULE_NAME = "user_raffle"
    }
}
