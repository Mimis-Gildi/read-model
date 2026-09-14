/*
 * The bench stylesheet, expressed in Silk. Rule for rule this is harness/bench.css.
 * Kobweb registers the same rules BUT through its own stylesheet.
 *
 * Hopefully as cheap as the other fixtures.
 */
package me.riddle.adventure.web.kobweb

import com.varabyte.kobweb.compose.css.*
import com.varabyte.kobweb.compose.css.AlignItems
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.init.registerStyleBase
import me.riddle.adventure.web.model.DOM_KEY_BUTTON
import org.jetbrains.compose.web.css.*

/** The bench palette screwed to both the `kobweb` light and dark theme. */
object Bench {
    val bg = Color.rgb(0x16161A)
    val panel = Color.rgb(0x1E1E24)
    val text = Color.rgb(0xE8E6E1)
    val muted = Color.rgb(0x8A8780)
    val accent = Color.rgb(0xC9955C)

    val font = arrayOf("ui-monospace", "SFMono-Regular", "Menlo", "monospace")
}

/** The shared cell padding of the results table, written once because th and td share it. */
private val cell = Modifier.padding(top = 0.2.cssRem, right = 1.5.cssRem, bottom = 0.2.cssRem, left = 0.px)

@InitSilk
fun initSiteStyles(ctx: InitSilkContext) = ctx.stylesheet.run {
    registerStyleBase("*") { Modifier.boxSizing(BoxSizing.BorderBox) }

    registerStyleBase("body") {
        Modifier
            .margin(0.px)
            .backgroundColor(Bench.bg)
            .color(Bench.text)
            .fontFamily(*Bench.font)
            .fontSize(13.px)
            .lineHeight(1.5)
    }

    registerStyleBase("header") {
        Modifier
            .position(Position.Sticky)
            .top(0.px)
            .zIndex(1)
            .display(DisplayStyle.Flex)
            .alignItems(AlignItems.Center)
            .gap(1.5.cssRem)
            .padding(topBottom = 0.75.cssRem, leftRight = 1.cssRem)
            .backgroundColor(Bench.panel)
    }

    registerStyleBase("h1") { Modifier.margin(0.px).fontSize(14.px).color(Bench.accent) }

    registerStyleBase("dl") {
        Modifier.display(DisplayStyle.Flex).alignItems(AlignItems.Baseline).gap(0.5.cssRem).margin(0.px)
    }
    registerStyleBase("dt") { Modifier.color(Bench.muted) }
    registerStyleBase("dd") { Modifier.margin(top = 0.px, right = 1.cssRem, bottom = 0.px, left = 0.px) }

    registerStyleBase("#results") {
        Modifier.margin(topBottom = 0.px, leftRight = 1.cssRem).borderCollapse(BorderCollapse.Collapse)
    }
    registerStyleBase("#results th") {
        cell.color(Bench.muted).fontWeight(FontWeight.Normal).textAlign(TextAlign.Left)
    }
    registerStyleBase("#results td") { cell }
    // The two measured columns, so the numbers read apart from the counts beside them.
    registerStyleBase("#results td:nth-child(4)") { Modifier.color(Bench.accent) }
    registerStyleBase("#results td:nth-child(5)") { Modifier.color(Bench.accent) }

    registerStyleBase(DOM_KEY_BUTTON) {
        Modifier
            .padding(topBottom = 0.35.cssRem, leftRight = 1.cssRem)
            .border(1.px, LineStyle.Solid, Bench.accent)
            .borderRadius(3.px)
            .backgroundColor(Colors.Transparent)
            .color(Bench.accent)
            .fontFamily(*Bench.font)
            .fontSize(13.px)
            .cursor(Cursor.Pointer)
    }
    registerStyleBase("$DOM_KEY_BUTTON:disabled") {
        Modifier.border(1.px, LineStyle.Solid, Bench.muted).color(Bench.muted).cursor(Cursor.Default)
    }

    registerStyleBase("#status") { Modifier.margin(0.px).color(Bench.muted) }
    registerStyleBase("main") { Modifier.padding(1.cssRem) }

    registerStyleBase(".kids") { Modifier.padding(left = 1.25.cssRem) }
    registerStyleBase(".node.collapsed > .kids") { Modifier.display(DisplayStyle.None) }
    registerStyleBase(".row") { Modifier.display(DisplayStyle.Flex).gap(0.5.cssRem).cursor(Cursor.Default) }
    registerStyleBase(".twist") { Modifier.width(1.ch).color(Bench.accent) }
    registerStyleBase(".meta") { Modifier.color(Bench.muted) }

    // The leaf, signified with CSS only and no extra element per person.
    registerStyleBase(".depth-3 .twist") { Modifier.color(Bench.muted) }
    registerStyleBase(".depth-3 .name") { Modifier.minWidth(22.ch) }
    // The padding in the label is the column layout; without `pre` the browser collapses it away.
    registerStyleBase(".depth-3 .meta") { Modifier.whiteSpace(WhiteSpace.Pre) }
}
