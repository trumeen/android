package io.homeassistant.companion.android.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.homeassistant.companion.android.R

class BatterySensorManager : SensorManager {

    companion object {
        private const val TAG = "BatterySensor"
        private val batteryLevel = SensorManager.BasicSensor(
            "battery_level",
            "sensor",
            R.string.basic_sensor_name_battery_level,
            R.string.sensor_description_battery_level,
            "battery",
            "%"
        )
        private val batteryState = SensorManager.BasicSensor(
            "battery_state",
            "sensor",
            R.string.basic_sensor_name_battery_state,
            R.string.sensor_description_battery_state,
            "battery"
        )
    }

    override val name: Int
        get() = R.string.sensor_name_battery
    override val availableSensors: List<SensorManager.BasicSensor>
        get() = listOf(batteryLevel, batteryState)

    override fun requiredPermissions(): Array<String> {
        return emptyArray()
    }

    override fun requestSensorUpdate(
        context: Context
    ) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent != null) {
            updateBatteryLevel(context, intent)
            updateBatteryState(context, intent)
        }
    }

    private fun getBatteryPercentage(intent: Intent): Int {
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return (level.toFloat() / scale.toFloat() * 100.0f).toInt()
    }

    private fun getBatteryIcon(
        percentage: Int,
        isCharging: Boolean = false,
        chargerType: String? = null,
        chargingStatus: String? = null
    ): String {
        var batteryIcon = "mdi:battery"

        if (chargingStatus == "unknown") {
            batteryIcon += "-unknown"

            return batteryIcon
        }

        if (isCharging)
            batteryIcon += "-charging"

        if (chargerType == "wireless")
            batteryIcon += "-wireless"

        val batteryStep: Int = percentage / 10
        batteryIcon += when (batteryStep) {
            0 -> "-outline"
            10 -> ""
            else -> "-${batteryStep}0"
        }

        return batteryIcon
    }

    private fun updateBatteryLevel(context: Context, intent: Intent) {
        if (!isEnabled(context, batteryLevel.id))
            return
        val percentage: Int = getBatteryPercentage(intent)
        val isCharging = getIsCharging(intent)
        val chargerType = getChargerType(intent)
        val chargingStatus = getChargingStatus(intent)

        onSensorUpdated(
            context,
            batteryLevel,
            percentage,
            getBatteryIcon(percentage, isCharging, chargerType, chargingStatus),
            mapOf()
        )
    }

    private fun updateBatteryState(context: Context, intent: Intent) {
        if (!isEnabled(context, batteryState.id))
            return

        val percentage: Int = getBatteryPercentage(intent)
        val isCharging = getIsCharging(intent)
        val chargerType = getChargerType(intent)
        val chargingStatus = getChargingStatus(intent)
        val batteryHealth = getBatteryHealth(intent)

        onSensorUpdated(
            context,
            batteryState,
            chargingStatus,
            getBatteryIcon(percentage, isCharging, chargerType, chargingStatus),
            mapOf(
                "is_charging" to isCharging,
                "charger_type" to chargerType,
                "battery_health" to batteryHealth
            )
        )
    }

    private fun getIsCharging(intent: Intent): Boolean {
        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getChargerType(intent: Intent): String {
        return when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "ac"
            BatteryManager.BATTERY_PLUGGED_USB -> "usb"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
            else -> "unknown"
        }
    }

    private fun getChargingStatus(intent: Intent): String {
        return when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
    }

    private fun getBatteryHealth(intent: Intent): String {
        return when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheated"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failed"
            else -> "unknown"
        }
    }
}
