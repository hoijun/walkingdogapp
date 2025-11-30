package com.tulmunchi.walkingdogapp.data.mapper

import com.tulmunchi.walkingdogapp.datamodel.AlarmDataModel
import com.tulmunchi.walkingdogapp.domain.model.Alarm

/**
 * Mapper between AlarmDataModel (Room) and Alarm domain model
 */
object AlarmMapper {
    fun toDomain(entity: AlarmDataModel): Alarm = Alarm(
        alarmCode = entity.alarm_code,
        time = entity.time,
        weeks = entity.weeks.toList(),
        isEnabled = entity.alarmOn
    )

    fun toEntity(domain: Alarm): AlarmDataModel = AlarmDataModel(
        alarm_code = domain.alarmCode,
        time = domain.time,
        weeks = domain.weeks.toTypedArray(),
        alarmOn = domain.isEnabled
    )

    fun toDomainList(entities: List<AlarmDataModel>): List<Alarm> = entities.map { toDomain(it) }

    fun toEntityList(domains: List<Alarm>): List<AlarmDataModel> = domains.map { toEntity(it) }
}
