package dev.keyup.recruiterbot.usage

data class ApiUsageEvent(
    val completedAtMillis: Long,
    val requestId: String,
    val method: String,
    val routeTemplate: String,
    val statusCode: Int,
    val durationMillis: Long,
)

interface ApiUsageRecorder {
    fun record(requestId: String, method: String, routeTemplate: String, statusCode: Int, durationMillis: Long)
}

data class UsageStatisticsResponse(
    val from: String,
    val to: String,
    val totalRequests: Long,
    val errorCount: Long,
    val errorRatePercent: Double,
    val statusBreakdown: List<StatusUsage>,
    val routeBreakdown: List<RouteUsage>,
    val latencyMillis: LatencyUsage,
)

data class StatusUsage(val statusCode: Int, val count: Long)
data class RouteUsage(val routeTemplate: String, val count: Long, val errorCount: Long)
data class LatencyUsage(val average: Double, val p95: Long, val maximum: Long)
