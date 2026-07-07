package cn.chahuyun.economy.data.proxy.module

import cn.chahuyun.economy.converter.v1.UserFactorV1Converter
import cn.chahuyun.economy.converter.v2.UserFactorV2Converter
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxy
import cn.chahuyun.economy.data.proxy.MigrationResult
import cn.chahuyun.economy.data.repository.UserFactorRepository
import cn.chahuyun.economy.model.user.UserFactorDto
import cn.chahuyun.economy.utils.Log

class UserFactorEntityProxy : EntityProxy<UserFactorDto> {

    private val v1Converter = UserFactorV1Converter()
    private val v2Converter = UserFactorV2Converter()

    override fun getModuleName(): String = MODULE_NAME

    override fun getCurrentVersion(): DataVersion = DataSourceStrategyImpl.getVersion(MODULE_NAME)

    override fun findById(id: Long): UserFactorDto? = when (getCurrentVersion()) {
        DataVersion.V2 -> findV2ByUserId(id)
        else -> findV1ById(id)
    }

    override fun findAll(): List<UserFactorDto> = when (getCurrentVersion()) {
        DataVersion.V2 -> findAllV2()
        else -> findAllV1()
    }

    override fun findWhere(predicate: (UserFactorDto) -> Boolean): List<UserFactorDto> = findAll().filter(predicate)

    override fun save(dto: UserFactorDto): UserFactorDto = when (getCurrentVersion()) {
        DataVersion.V2 -> saveV2(dto)
        else -> saveV1(dto)
    }

    override fun saveAll(dtos: List<UserFactorDto>): List<UserFactorDto> = dtos.map { save(it) }

    override fun delete(id: Long): Boolean = when (getCurrentVersion()) {
        DataVersion.V2 -> deleteV2ByUserId(id)
        else -> deleteV1(id)
    }

    override fun migrateTo(targetVersion: DataVersion): MigrationResult {
        return when (targetVersion) {
            DataVersion.V2 -> migrateV1ToV2()
            DataVersion.V1 -> MigrationResult.success(0)
            else -> MigrationResult.failure(0, 0, listOf("UserFactorEntityProxy does not support migration to $targetVersion yet"))
        }
    }

    private fun findV1ById(id: Long): UserFactorDto? = try {
        UserFactorRepository.findById(id)?.let { v1Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy query V1 factor failed: id=$id", e)
        null
    }

    private fun findAllV1(): List<UserFactorDto> = try {
        v1Converter.toDtoList(UserFactorRepository.listAll())
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy query all V1 factors failed", e)
        emptyList()
    }

    private fun saveV1(dto: UserFactorDto): UserFactorDto = try {
        v1Converter.toDto(UserFactorRepository.save(v1Converter.toEntity(dto)))
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy save V1 factor failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV1(id: Long): Boolean = try {
        UserFactorRepository.deleteById(id)
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy delete V1 factor failed: id=$id", e)
        false
    }

    private fun findV2ByUserId(userId: Long): UserFactorDto? = try {
        UserFactorRepository.findV2ByUserId(userId)?.let { v2Converter.toDto(it) }
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy query V2 factor failed: userId=$userId", e)
        null
    }

    private fun findAllV2(): List<UserFactorDto> = try {
        v2Converter.toDtoList(UserFactorRepository.listAllV2())
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy query all V2 factors failed", e)
        emptyList()
    }

    private fun saveV2(dto: UserFactorDto): UserFactorDto = try {
        val entity = v2Converter.toEntity(dto)
        v2Converter.toDto(UserFactorRepository.saveV2(entity))
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy save V2 factor failed: id=${dto.id}", e)
        dto
    }

    private fun deleteV2ByUserId(userId: Long): Boolean = try {
        UserFactorRepository.deleteV2ByUserId(userId)
    } catch (e: Exception) {
        Log.error("UserFactorEntityProxy delete V2 factor failed: userId=$userId", e)
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
        private const val MODULE_NAME = "user_factor"
    }
}
