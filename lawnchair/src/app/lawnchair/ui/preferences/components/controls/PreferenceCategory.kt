/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.components.controls

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.subRoute
import app.lawnchair.ui.theme.LawnchairTheme
import app.lawnchair.ui.util.PreviewLawnchair
import com.android.launcher3.R

@Composable
fun PreferenceCategory(
    label: String,
    @DrawableRes iconResource: Int,
    route: String,
    description: String? = null,
) {
    val navController = LocalNavController.current
    val resolvedRoute = subRoute(name = route)

    PreferenceCategory(
        label = label,
        iconResource = iconResource,
        onNavigate = { navController.navigate(resolvedRoute) },
        description = description,
    )
}

@Composable
private fun PreferenceCategory(
    label: String,
    @DrawableRes iconResource: Int,
    onNavigate: () -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        PreferenceTemplate(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) { onNavigate() },
            verticalPadding = 14.dp,
            title = {
                Text(text = label)
            },
            description = {
                if (description != null) {
                    Text(text = description)
                }
            },
            startWidget = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = iconResource),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
    }
}

@PreviewLawnchair
@Composable
private fun PreferenceCategoryPreview() {
    LawnchairTheme {
        PreferenceCategory(
            label = "Example",
            description = "Example description here",
            iconResource = R.drawable.ic_general,
            onNavigate = {},
        )
    }
}