/*
 * The Kobweb fixture page. Empty on purpose: the page itself is static HTML shared with every fixture,
 * and `KobwebFixture` renders into its `#tree`.
 */
package me.riddle.adventure.web.kobweb.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page

/**
 * This page is intentionally BLANK.
 * :service index.html is used instead:
 * that assured parity with other fixtures
 * considering cost of styling and generation
 * quirks of kobweb prettiness nat required.
 *
 * Generated index.html is stripped out during
 * `export` operation used in Gradle build phase.
 */
@Page
@Composable
fun HomePage() {
}
