package com.minifocus.launcher.manager

import com.minifocus.launcher.data.dao.AppUsageStatsDao
import com.minifocus.launcher.data.entity.AppUsageStatsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.math.pow

private const val DAY_IN_MILLIS = 86_400_000L
private const val DECAY_FACTOR = 0.92

class AppUsageStatsManager(
    private val dao: AppUsageStatsDao,
    private val scope: CoroutineScope,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    private val stats = MutableStateFlow<Map<String, AppUsageStats>>(emptyMap())
    private val mutex = Mutex()

    init {
        scope.launch(Dispatchers.IO) {
            val snapshot = dao.getAll().associateBy { it.packageName }
                .mapValues { (_, entity) -> entity.toModel() }
            stats.value = snapshot
        }
    }

    fun observeStats(): StateFlow<Map<String, AppUsageStats>> = stats.asStateFlow()

    suspend fun recordLaunch(packageName: String, weight: Double) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val dayIndex = (now / DAY_IN_MILLIS).toInt()
            val cohort = AppUsageCohort.fromHour(currentHour(now)).index
            val current = stats.value[packageName] ?: AppUsageStats.blank(packageName, dayIndex)
            val decayed = current.decayTo(dayIndex)
            val updated = decayed.increment(weight, cohort, now)
            withContext(Dispatchers.IO) {
                dao.upsert(updated.toEntity())
            }
            stats.update { it + (packageName to updated) }
        }
    }

    suspend fun clearAll() {
        mutex.withLock {
            withContext(Dispatchers.IO) { dao.clearAll() }
            stats.value = emptyMap()
        }
    }

    private fun currentHour(now: Long): Int {
        return Instant.ofEpochMilli(now).atZone(zoneId).hour
    }
}

data class AppUsageStats(
    val packageName: String,
    val totalScore: Double,
    val cohortScores: DoubleArray,
    val lastLaunchEpoch: Long,
    val lastDecayDay: Int
) {
    fun toEntity(): AppUsageStatsEntity {
        return AppUsageStatsEntity(
            packageName = packageName,
            totalScore = totalScore,
            morningScore = cohortScores.getOrNull(AppUsageCohort.MORNING.index) ?: 0.0,
            middayScore = cohortScores.getOrNull(AppUsageCohort.MIDDAY.index) ?: 0.0,
            eveningScore = cohortScores.getOrNull(AppUsageCohort.EVENING.index) ?: 0.0,
            lateScore = cohortScores.getOrNull(AppUsageCohort.LATE.index) ?: 0.0,
            lastLaunchEpoch = lastLaunchEpoch,
            lastDecayDay = lastDecayDay
        )
    }

    fun decayTo(targetDay: Int): AppUsageStats {
        if (targetDay <= lastDecayDay) return this
        val gap = targetDay - lastDecayDay
        val factor = DECAY_FACTOR.pow(gap)
        val decayedCohorts = DoubleArray(cohortScores.size) { index -> cohortScores[index] * factor }
        return copy(
            totalScore = totalScore * factor,
            cohortScores = decayedCohorts,
            lastDecayDay = targetDay
        )
    }

    fun increment(weight: Double, cohortIndex: Int, now: Long): AppUsageStats {
        val updatedCohorts = cohortScores.copyOf()
        if (cohortIndex in updatedCohorts.indices) {
            updatedCohorts[cohortIndex] = updatedCohorts[cohortIndex] + weight
        }
        return copy(
            totalScore = totalScore + weight,
            cohortScores = updatedCohorts,
            lastLaunchEpoch = now
        )
    }

    companion object {
        fun blank(packageName: String, dayIndex: Int): AppUsageStats {
            return AppUsageStats(
                packageName = packageName,
                totalScore = 0.0,
                cohortScores = DoubleArray(AppUsageCohort.values().size) { 0.0 },
                lastLaunchEpoch = 0L,
                lastDecayDay = dayIndex
            )
        }
    }
}

enum class AppUsageCohort(val index: Int) {
    MORNING(0),
    MIDDAY(1),
    EVENING(2),
    LATE(3);

    companion object {
        fun fromHour(hour: Int): AppUsageCohort {
            return when (hour) {
                in 5..9 -> MORNING
                in 10..15 -> MIDDAY
                in 16..20 -> EVENING
                else -> LATE
            }
        }
    }
}

private fun AppUsageStatsEntity.toModel(): AppUsageStats {
    return AppUsageStats(
        packageName = packageName,
        totalScore = totalScore,
        cohortScores = doubleArrayOf(morningScore, middayScore, eveningScore, lateScore),
        lastLaunchEpoch = lastLaunchEpoch,
        lastDecayDay = lastDecayDay
    )
}