package cn.chahuyun.economy.converter

/**
 * 实体-DTO双向转换器接口
 *
 * 每个实体版本（V1/V2/V3）都需要实现此接口，负责实体与DTO之间的双向转换。
 *
 * @param E 实体类型
 * @param D DTO类型
 */
interface Converter<E, D> {

    /**
     * 实体 → DTO
     */
    fun toDto(entity: E): D

    /**
     * DTO → 实体
     */
    fun toEntity(dto: D): E

    /**
     * 批量 实体 → DTO
     */
    fun toDtoList(entities: List<E>): List<D> = entities.map { toDto(it) }

    /**
     * 批量 DTO → 实体
     */
    fun toEntityList(dtos: List<D>): List<E> = dtos.map { toEntity(it) }
}
