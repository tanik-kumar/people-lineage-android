package com.tanik.peoplelineage.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.tanik.peoplelineage.R
import com.tanik.peoplelineage.data.PeopleRepository
import com.tanik.peoplelineage.databinding.ActivityHierarchyBinding
import com.tanik.peoplelineage.model.LineageGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class HierarchyActivity : ComponentActivity() {

    private lateinit var binding: ActivityHierarchyBinding
    private lateinit var repository: PeopleRepository
    private var personId: Long = 0L
    private var currentGraph: LineageGraph? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHierarchyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = PeopleRepository.getInstance(this)
        personId = intent.getLongExtra(EXTRA_PERSON_ID, 0L)

        binding.toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.shareGraphButton.setOnClickListener { shareGraphPdf() }
    }

    override fun onResume() {
        super.onResume()
        loadGraph()
    }

    private fun loadGraph() {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.buildLineageGraph(personId)
                }
            }.onSuccess { graph ->
                if (graph == null || graph.nodes.isEmpty()) {
                    currentGraph = null
                    binding.graphEmptyText.isVisible = true
                    binding.familyTreeView.isVisible = false
                    binding.shareGraphButton.isEnabled = false
                } else {
                    currentGraph = graph
                    binding.graphEmptyText.isVisible = false
                    binding.familyTreeView.isVisible = true
                    binding.shareGraphButton.isEnabled = true
                    binding.toolbar.subtitle = graph.nodes.firstOrNull { it.isFocus }?.name
                    binding.familyTreeView.setGraph(graph)
                }
            }.onFailure {
                Snackbar.make(
                    binding.root,
                    it.message ?: getString(R.string.operation_failed),
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun shareGraphPdf() {
        val graph = currentGraph ?: return
        runCatching {
            val pdfFile = createGraphPdf(graph)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", pdfFile)
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_graph_subject, graph.nodes.firstOrNull { it.isFocus }?.name.orEmpty()))
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    getString(R.string.share_graph_pdf),
                ),
            )
        }.onFailure {
            Snackbar.make(binding.root, getString(R.string.share_failed), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun createGraphPdf(graph: LineageGraph): File {
        val sharedDir = File(cacheDir, "shared").apply { mkdirs() }
        val file = File(sharedDir, getString(R.string.pdf_file_name, graph.focusPersonId))
        val document = PdfDocument()
        val exportView = createExportGraphView(graph)
        val contentWidth = exportView.getContentWidthPx().coerceAtLeast(1)
        val contentHeight = exportView.getContentHeightPx().coerceAtLeast(1)
        val isLandscape = contentWidth >= contentHeight
        val basePageWidth = if (isLandscape) 842 else 595
        val basePageHeight = if (isLandscape) 595 else 842
        val margin = 32
        val pageWidth = (contentWidth + (margin * 2)).coerceAtLeast(basePageWidth).coerceAtMost(MAX_PDF_PAGE_EDGE)
        val pageHeight = (contentHeight + (margin * 2)).coerceAtLeast(basePageHeight).coerceAtMost(MAX_PDF_PAGE_EDGE)
        val drawableWidth = pageWidth - (margin * 2)
        val drawableHeight = pageHeight - (margin * 2)
        val scale = minOf(
            drawableWidth.toFloat() / contentWidth.toFloat(),
            drawableHeight.toFloat() / contentHeight.toFloat(),
            1f,
        )
        val renderedWidth = contentWidth * scale
        val renderedHeight = contentHeight * scale
        val left = margin + ((drawableWidth - renderedWidth) / 2f)
        val top = margin + ((drawableHeight - renderedHeight) / 2f)

        val page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create(),
        )
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        exportView.draw(canvas)
        canvas.restore()
        document.finishPage(page)

        FileOutputStream(file).use { output ->
            document.writeTo(output)
        }
        document.close()
        return file
    }

    private fun createExportGraphView(graph: LineageGraph): com.tanik.peoplelineage.widget.FamilyTreeView {
        return com.tanik.peoplelineage.widget.FamilyTreeView(this).apply {
            setBackgroundColor(Color.WHITE)
            setGraph(graph)
            val contentWidth = getContentWidthPx()
            val contentHeight = getContentHeightPx()
            measure(
                android.view.View.MeasureSpec.makeMeasureSpec(contentWidth, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(contentHeight, android.view.View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, contentWidth, contentHeight)
        }
    }

    companion object {
        private const val EXTRA_PERSON_ID = "extra_person_id"
        private const val MAX_PDF_PAGE_EDGE = 4096

        fun createIntent(context: Context, personId: Long): Intent {
            return Intent(context, HierarchyActivity::class.java).apply {
                putExtra(EXTRA_PERSON_ID, personId)
            }
        }
    }
}
