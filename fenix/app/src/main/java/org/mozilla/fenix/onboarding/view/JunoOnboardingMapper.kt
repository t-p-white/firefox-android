/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.view

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import org.mozilla.fenix.R
import org.mozilla.fenix.nimbus.OnboardingCardData
import org.mozilla.fenix.nimbus.OnboardingCardType
import org.mozilla.fenix.settings.SupportUtils

/**
 * Mapper to convert [OnboardingCardData] to [OnboardingPageState] that is a param for
 * [OnboardingPage] composable.
 */
@ReadOnlyComposable
@Composable
@Suppress("LongParameterList")
internal fun mapToOnboardingPageState(
    onboardingCardData: OnboardingCardData,
    onMakeFirefoxDefaultClick: () -> Unit,
    onMakeFirefoxDefaultSkipClick: () -> Unit,
    onPrivacyPolicyClick: (String) -> Unit,
    onSignInButtonClick: () -> Unit,
    onSignInSkipClick: () -> Unit,
    onNotificationPermissionButtonClick: () -> Unit,
    onNotificationPermissionSkipClick: () -> Unit,
): OnboardingPageState {
    return when (onboardingCardData.cardType) {
        OnboardingCardType.DEFAULT_BROWSER -> createOnboardingPageState(
            onboardingCardData = onboardingCardData,
            imageResource = R.drawable.ic_onboarding_welcome,
            onPositiveButtonClick = onMakeFirefoxDefaultClick,
            onNegativeButtonClick = onMakeFirefoxDefaultSkipClick,
            onUrlClick = onPrivacyPolicyClick,
        )

        OnboardingCardType.SYNC_SIGN_IN -> createOnboardingPageState(
            onboardingCardData = onboardingCardData,
            imageResource = R.drawable.ic_onboarding_sync,
            onPositiveButtonClick = onSignInButtonClick,
            onNegativeButtonClick = onSignInSkipClick,
        )

        OnboardingCardType.NOTIFICATION_PERMISSION -> createOnboardingPageState(
            onboardingCardData = onboardingCardData,
            imageResource = R.drawable.ic_notification_permission,
            onPositiveButtonClick = onNotificationPermissionButtonClick,
            onNegativeButtonClick = onNotificationPermissionSkipClick,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun createOnboardingPageState(
    onboardingCardData: OnboardingCardData,
    @DrawableRes imageResource: Int,
    onPositiveButtonClick: () -> Unit,
    onNegativeButtonClick: () -> Unit,
    onUrlClick: (String) -> Unit = {},
): OnboardingPageState {
    return OnboardingPageState(
        image = imageResource,
        title = onboardingCardData.title,
        description = onboardingCardData.body ?: defaultBrowserCardBody(),
        primaryButton = Action(
            text = onboardingCardData.primaryButton,
            onClick = onPositiveButtonClick,
        ),
        secondaryButton = Action(
            text = onboardingCardData.secondaryButton,
            onClick = onNegativeButtonClick,
        ),
        linkTextState = LinkTextState(
            text = stringResource(id = R.string.juno_onboarding_default_browser_description_link_text),
            url = SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVATE_NOTICE),
            onClick = onUrlClick,
        ),
        onRecordImpressionEvent = {},
    )
}

@Composable
@ReadOnlyComposable
private fun defaultBrowserCardBody() = stringResource(
    id = R.string.juno_onboarding_default_browser_description,
    formatArgs = arrayOf(
        stringResource(R.string.firefox),
        stringResource(R.string.juno_onboarding_default_browser_description_link_text),
    ),
)
