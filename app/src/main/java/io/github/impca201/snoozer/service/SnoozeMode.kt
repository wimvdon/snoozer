package io.github.impca201.snoozer.service

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import io.github.impca201.snoozer.R
import io.github.impca201.snoozer.ui.MainActivity

/**
 * Optional Android 15+ "mode" (an AutomaticZenRule owned by Snoozer) that is switched on while
 * snoozing. Snoozer creates it with Android's defaults and never changes what it does: the user
 * configures it under Settings › Modes (dim wallpaper, dark theme, who may interrupt, …) and can
 * combine it with other modes. Only the name is managed here. Switching the setting off deletes it.
 */
object SnoozeMode {
    // Kept from the first version so modes created back then keep working.
    private val CONDITION_ID: Uri = "snoozer://mode/dim".toUri()

    @Volatile private var lastRuleId: String? = null

    @Volatile private var lastActive: Boolean? = null

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    /** Managing modes needs notification policy ("Do Not Disturb") access. */
    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    /** Creates, (de)activates or removes the mode so it matches the current state. */
    fun apply(
        context: Context,
        enabled: Boolean,
        active: Boolean,
        name: String,
    ) {
        if (!isSupported()) return
        applyApi35(context, enabled, active, name.ifBlank { context.getString(R.string.app_name) })
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun applyApi35(
        context: Context,
        enabled: Boolean,
        active: Boolean,
        name: String,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.isNotificationPolicyAccessGranted) return
        try {
            // Only rules owned by this app are returned.
            val existingId = manager.automaticZenRules.keys.firstOrNull()
            if (!enabled) {
                existingId?.let(manager::removeAutomaticZenRule)
                lastRuleId = null
                return
            }
            val id = existingId ?: manager.addAutomaticZenRule(buildRule(context, name)) ?: return
            // Every write changes the Do Not Disturb configuration (and wakes automation apps
            // watching it), so only send the state when it actually changes.
            if (id == lastRuleId && active == lastActive) return
            val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
            manager.setAutomaticZenRuleState(
                id,
                Condition(CONDITION_ID, context.getString(R.string.mode_trigger_description), state),
            )
            lastRuleId = id
            lastActive = active
        } catch (error: RuntimeException) {
            // OEM builds or user-managed modes can refuse changes; the mode is a nice-to-have.
            Log.w("Snoozer", "Could not update the Snoozer mode", error)
        }
    }

    /**
     * Renames the mode. Called only when the user saves a new name in Snoozer; Android keeps a
     * name the user set in Settings › Modes, in which case this has no effect.
     */
    fun rename(
        context: Context,
        name: String,
    ) {
        if (!isSupported()) return
        renameApi35(context, name.ifBlank { context.getString(R.string.app_name) })
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun renameApi35(
        context: Context,
        name: String,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.isNotificationPolicyAccessGranted) return
        try {
            val (id, rule) = manager.automaticZenRules.entries.firstOrNull() ?: return
            if (rule.name != name) {
                manager.updateAutomaticZenRule(id, AutomaticZenRule.Builder(rule).setName(name).build())
            }
        } catch (error: RuntimeException) {
            Log.w("Snoozer", "Could not rename the Snoozer mode", error)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun buildRule(
        context: Context,
        name: String,
    ): AutomaticZenRule =
        // Android's defaults on purpose: no interruption filter or device effects of our own.
        AutomaticZenRule
            .Builder(name, CONDITION_ID)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .setConfigurationActivity(ComponentName(context, MainActivity::class.java))
            .setIconResId(R.drawable.ic_tile)
            .setTriggerDescription(context.getString(R.string.mode_trigger_description))
            .setManualInvocationAllowed(false)
            .build()
}
