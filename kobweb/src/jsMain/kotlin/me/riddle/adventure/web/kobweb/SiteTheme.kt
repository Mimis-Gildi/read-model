/*
 * Silk's palette, pinned to the bench palette.
 */
package me.riddle.adventure.web.kobweb

import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.theme.colors.palette.background
import com.varabyte.kobweb.silk.theme.colors.palette.color

@InitSilk
fun initTheme(ctx: InitSilkContext) = ctx.theme.palettes.run {
    listOf(light, dark).forEach { palette ->
        palette.background = Bench.bg
        palette.color = Bench.text
    }
}
