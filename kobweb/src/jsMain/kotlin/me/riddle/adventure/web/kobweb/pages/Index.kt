/*
 * The Kobweb fixture, launched by the control plane as /?run=<uuid>&module=kobweb&dataset=<key>.
 *
 * CAUTION: `#tree` is left empty by this composition on purpose. The harness mounts its own composition root into it,
 * so that recomposing a status line can never touch the thing under the clock.
 */
package me.riddle.adventure.web.kobweb.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.riddle.adventure.web.kobweb.bench.Harness
import me.riddle.adventure.web.kobweb.bench.Launch
import me.riddle.adventure.web.kobweb.bench.Reading
import me.riddle.adventure.web.kobweb.bench.Rung
import me.riddle.adventure.web.kobweb.bench.count
import me.riddle.adventure.web.kobweb.bench.ms
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.DDescription
import org.jetbrains.compose.web.dom.DList
import org.jetbrains.compose.web.dom.DTerm
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Tbody
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Thead
import org.jetbrains.compose.web.dom.Tr

private const val DASH = "–"

private class Report(
    val nodes: String = DASH,
    val elements: String = DASH,
    val built: String = DASH,
    val painted: String = DASH,
) {
    val cells: List<String> get() = listOf(nodes, elements, built, painted)
}

private fun host() = document.getElementById("tree") ?: error("the fixture page has no #tree")

@Composable
private fun Term(label: String, value: String, id: String) {
    DTerm { Text(label) }
    DDescription(attrs = { id(id) }) { Text(value) }
}

@Composable
private fun Results(reports: Map<Rung, Report>) {
    Table(attrs = { id("results") }) {
        Thead {
            Tr {
                listOf("Rung", "Nodes", "Elements", "Built", "Painted")
                    .forEach { heading -> Th { Text(heading) } }
            }
        }
        Tbody(attrs = { id("rows") }) {
            Rung.entries.forEach { rung ->
                Tr(attrs = { id(rung.id) }) {
                    Td { Text(rung.title) }
                    reports.getOrElse(rung) { Report() }.cells
                        .forEach { cell -> Td { Text(cell) } }
                }
            }
        }
    }
}

@Page
@Composable
fun HomePage() {
    val launch = remember { Launch.read() }

    // #tree only exists once this composition has been committed, and the socket opens with the harness.
    val harness = remember { lazy { Harness(launch, host()) } }
    val scope = rememberCoroutineScope()

    val reports = remember { mutableStateMapOf<Rung, Report>() }
    var status by remember { mutableStateOf(if (launch.dataset.isEmpty()) "No dataset on the URL." else "Ready.") }
    var busy by remember { mutableStateOf(false) }
    var laddered by remember { mutableStateOf(false) }

    val fill = { rung: Rung, nodes: Int, reading: Reading ->
        reports[rung] = Report(
            nodes = count(nodes),
            elements = count(reading.elements),
            built = ms(reading.built),
            painted = ms(reading.painted),
        )
    }

    val run = { work: suspend (Harness) -> Unit ->
        busy = true
        scope.launch {
            runCatching { work(harness.value) }
                .onFailure { status = "Failed: ${it.message}" }
                .also { busy = false }
        }
        Unit
    }

    Header {
        H1 { Text("Kobweb") }
        DList {
            Term("run", launch.run.ifEmpty { DASH }, "run")
            Term("dataset", launch.dataset.ifEmpty { DASH }, "datasetKey")
        }
        Button(attrs = {
            id("start")
            if (busy || launch.dataset.isEmpty()) disabled()
            onClick {
                reports.clear()
                laddered = false
                run { it.ladder({ line -> status = line }, fill).also { laddered = true } }
            }
        }) { Text("Run ladder") }
        Button(attrs = {
            id("expandAll")
            if (busy || !laddered) disabled()
            onClick { run { it.reveal({ line -> status = line }, fill) } }
        }) { Text("Expand all") }
        Button(attrs = {
            id("collapseAll")
            if (busy || !laddered) disabled()
            onClick { run { it.collapseAll { line -> status = line } } }
        }) { Text("Collapse all") }
        P(attrs = { id("status") }) { Text(status) }
    }

    Results(reports)

    Main(attrs = { id("tree") })
}
