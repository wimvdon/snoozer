package io.github.impca201.snoozer.service

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenDeviceEffects
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.ui.MainActivity

/**
 * Optional Android 15+ "mode" (an AutomaticZenRule owned by Snoozer) that lets every
 * notification through and only dims the wallpaper while snoozing: a native visual cue.
 * It exists only while the setting is on; switching the setting off deletes it again.
 */
object SnoozeMode {
    private val CONDITION_ID: Uri = "snoozer://mode/dim".toUri()

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    /** Managing modes needs notification policy ("Do Not Disturb") access. */
    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    /** Creates, (de)activates or removes the mode so it matches the current state. */
    fun apply(
        context: Context,
        enabled: Boolean,
        active: Boolean,
    ) {
        if (!isSupported()) return
        applyApi35(context, enabled, active)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun applyApi35(
        context: Context,
        enabled: Boolean,
        active: Boolean,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.isNotificationPolicyAccessGranted) return
        try {
            // Only rules owned by this app are returned.
            val existingId = manager.automaticZenRules.keys.firstOrNull()
            if (!enabled) {
                existingId?.let(manager::removeAutomaticZenRule)
                return
            }
            // Never overwrite an existing rule: the user may have customised it in Settings.
            val id = existingId ?: manager.addAutomaticZenRule(buildRule(context)) ?: return
            val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
            manager.setAutomaticZenRuleState(
                id,
                Condition(CONDITION_ID, context.getString(R.string.mode_trigger_description), state),
            )
        } catch (error: RuntimeException) {
            // OEM builds or user-managed modes can refuse changes; the mode is a nice-to-have.
            Log.w("Snoozer", "Could not update the Snoozer mode", error)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun buildRule(context: Context): AutomaticZenRule =
        AutomaticZenRule
            .Builder(context.getString(R.string.app_name), CONDITION_ID)
            .setType(AutomaticZenRule.TYPE_OTHER)
            // Let everything through: Snoozer does the snoozing, the mode only adds the effect.
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            .setDeviceEffects(ZenDeviceEffects.Builder().setShouldDimWallpaper(true).build())
            .setConfigurationActivity(ComponentName(context, MainActivity::class.java))
            .setIconResId(R.drawable.ic_tile)
            .setTriggerDescription(context.getString(R.string.mode_trigger_description))
            .setManualInvocationAllowed(false)
            .build()
}
