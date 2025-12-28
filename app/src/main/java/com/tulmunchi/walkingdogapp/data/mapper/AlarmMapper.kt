package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.data.source.local.entity.AlarmEntity
import com.tulmunchi.walkingdogapp.domain.model.Alarm

/**
 * Mapper between AlarmEntity (Room) and Alarm domain model
 */
object AlarmMapper {
    fun toDomain(entity: AlarmEntity): Alarm = Alarm(
        alarmCode = entity.alarm_code,
        time = entity.time,
        weeks = entity.weeks.toList(),
        isEnabled = entity.alarmOn
    )

    fun toEntity(domain: Alarm): AlarmEntity = AlarmEntity(
        alarm_code = domain.alarmCode,
        time = domain.time,
        weeks = domain.weeks.toTypedArray(),
        alarmOn = domain.isEnabled
    )

    fun toDomainList(entities: List<AlarmEntity>): List<Alarm> = entities.map { toDomain(it) }
}
