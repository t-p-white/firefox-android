/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.areNotificationsEnabledSafe
import org.mozilla.fenix.ext.hideToolbar
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.openSetDefaultBrowserOption
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.nimbus.OnboardingCardData
import org.mozilla.fenix.nimbus.OnboardingCardType
import org.mozilla.fenix.onboarding.view.JunoOnboardingScreen
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Fragment displaying the juno onboarding flow.
 */
class JunoOnboardingFragment : Fragment() {

    private val fenixOnboarding by lazy { FenixOnboarding(requireContext()) }
    private val onboardingCardsToDisplay by lazy { onboardingCardsToDisplay(requireContext()) }
    private val telemetryRecorder by lazy { JunoOnboardingTelemetryRecorder() }
    private val onboardingCardsTelemetrySequenceId by lazy { onboardingCardsToDisplay.telemetrySequenceId() }

    private val defaultBrowserCard by lazy {
        onboardingCardsToDisplay.first { it.cardType == OnboardingCardType.DEFAULT_BROWSER }
    }
    private val syncCard by lazy {
        onboardingCardsToDisplay.first { it.cardType == OnboardingCardType.SYNC_SIGN_IN }
    }

    /**
     * The notification card may not be required.
     */
    private val notificationCard by lazy {
        onboardingCardsToDisplay.find { it.cardType == OnboardingCardType.NOTIFICATION_PERMISSION }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isNotATablet()) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            FirefoxTheme {
                ScreenContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideToolbar()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isNotATablet()) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    private fun ScreenContent() {
        val context = LocalContext.current
        JunoOnboardingScreen(
            onboardingCardsToDisplay = onboardingCardsToDisplay,
            onMakeFirefoxDefaultClick = {
                activity?.openSetDefaultBrowserOption(useCustomTab = true)
                telemetryRecorder.onSetToDefaultClick(
                    sequenceId = onboardingCardsTelemetrySequenceId,
                    sequencePosition = defaultBrowserCard.telemetry.sequencePosition,
                )
            },
            onSkipDefaultClick = {
                telemetryRecorder.onSkipSetToDefaultClick(
                    onboardingCardsTelemetrySequenceId,
                    defaultBrowserCard.telemetry.sequencePosition,
                )
            },
            onPrivacyPolicyClick = { url ->
                startActivity(
                    SupportUtils.createSandboxCustomTabIntent(
                        context = context,
                        url = url,
                    ),
                )
                telemetryRecorder.onPrivacyPolicyClick(
                    onboardingCardsTelemetrySequenceId,
                    defaultBrowserCard.telemetry.sequencePosition,
                )
            },
            onSignInButtonClick = {
                findNavController().nav(
                    id = R.id.junoOnboardingFragment,
                    directions = JunoOnboardingFragmentDirections.actionGlobalTurnOnSync(),
                )
                telemetryRecorder.onSyncSignInClick(
                    sequenceId = onboardingCardsTelemetrySequenceId,
                    sequencePosition = syncCard.telemetry.sequencePosition,
                )
            },
            onSkipSignInClick = {
                telemetryRecorder.onSkipSignInClick(
                    onboardingCardsTelemetrySequenceId,
                    syncCard.telemetry.sequencePosition,
                )
            },
            onNotificationPermissionButtonClick = {
                notificationCard?.let {
                    requireComponents.notificationsDelegate.requestNotificationPermission()
                    telemetryRecorder.onNotificationPermissionClick(
                        sequenceId = onboardingCardsTelemetrySequenceId,
                        sequencePosition = it.telemetry.sequencePosition,
                    )
                }
            },
            onSkipNotificationClick = {
                notificationCard?.let {
                    telemetryRecorder.onSkipTurnOnNotificationsClick(
                        onboardingCardsTelemetrySequenceId,
                        it.telemetry.sequencePosition,
                    )
                }
            },
            onFinish = { onFinish(onboardingCardsTelemetrySequenceId, it) },
            onImpression = {
                telemetryRecorder.onImpression(
                    onboardingCardsTelemetrySequenceId,
                    it,
                )
            },
        )
    }

    private fun onFinish(sequenceId: String, onboardingCard: OnboardingCardData) {
        fenixOnboarding.finish()
        findNavController().nav(
            id = R.id.junoOnboardingFragment,
            directions = JunoOnboardingFragmentDirections.actionOnboardingHome(),
        )
        telemetryRecorder.onOnboardingComplete(
            sequenceId = sequenceId,
            sequencePosition = onboardingCard.telemetry.sequencePosition,
        )
    }

    private fun onboardingCardsToDisplay(context: Context): List<OnboardingCardData> {
        val allCards: Map<String, OnboardingCardData> =
            FxNimbus.features.junoOnboarding.value().cards
        return allCards.values
            .filter {
                if (it.cardType == OnboardingCardType.NOTIFICATION_PERMISSION) {
                    shouldShowNotificationPage(context)
                } else {
                    true
                }
            }
            .sortedBy { it.ordering }
    }

    private fun shouldShowNotificationPage(context: Context) =
        !NotificationManagerCompat.from(context.applicationContext)
            .areNotificationsEnabledSafe() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun isNotATablet() = !resources.getBoolean(R.bool.tablet)

    /**
     * Helper function for telemetry that maps List<OnboardingCardData> to a string of page names
     * separated by an underscore.
     * e.g. [DEFAULT_BROWSER, SYNC_SIGN_IN] will be mapped to "default_sync".
     */
    private fun List<OnboardingCardData>.telemetrySequenceId(): String =
        joinToString(separator = "_") { it.telemetry.id }
}
