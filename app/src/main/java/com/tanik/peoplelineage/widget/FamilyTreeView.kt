package com.tanik.peoplelineage.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.tanik.peoplelineage.model.LineageGraph
import com.tanik.peoplelineage.model.LineageGraphLink
import com.tanik.peoplelineage.model.LineageGraphLinkType
import com.tanik.peoplelineage.model.LineageGraphNode
import kotlin.math.abs
import kotlin.math.max

class FamilyTreeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val nodeWidth = 220f * density
    private val nodeHeight = 84f * density
    private val horizontalGap = 28f * density
    private val verticalGap = 92f * density
    private val contentPadding = 34f * density
    private val cornerRadius = 22f * density

    private val parentConnectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A9E97")
        strokeWidth = 3f * density
        style = Paint.Style.STROKE
    }
    private val spouseConnectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B86A2F")
        strokeWidth = 3f * density
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
    }
    private val regularNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val focusNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAF5F2")
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D8D9D1")
        strokeWidth = density
        style = Paint.Style.STROKE
    }
    private val focusBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#155E63")
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2933")
        textSize = 15f * density
        isFakeBoldText = true
    }
    private val locationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#52606D")
        textSize = 12f * density
    }

    private var graph: LineageGraph? = null
    private var nodeLayout: Map<Long, NodeLayout> = emptyMap()
    private var desiredContentWidth = 0f
    private var desiredContentHeight = 0f

    fun setGraph(graph: LineageGraph) {
        this.graph = graph
        rebuildLayout()
        requestLayout()
        invalidate()
    }

    fun getContentWidthPx(): Int {
        if (nodeLayout.isEmpty()) {
            rebuildLayout()
        }
        return desiredContentWidth.toInt().coerceAtLeast(1)
    }

    fun getContentHeightPx(): Int {
        if (nodeLayout.isEmpty()) {
            rebuildLayout()
        }
        return desiredContentHeight.toInt().coerceAtLeast(1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (nodeLayout.isEmpty()) {
            rebuildLayout()
        }
        val desiredWidth = (desiredContentWidth + paddingLeft + paddingRight).toInt()
        val desiredHeight = (desiredContentHeight + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val graphValue = graph ?: return
        if (graphValue.nodes.isEmpty()) {
            return
        }

        graphValue.links.forEach { link ->
            when (link.type) {
                LineageGraphLinkType.PARENT_CHILD -> drawParentChildLink(canvas, link)
                LineageGraphLinkType.SPOUSE -> drawSpouseLink(canvas, link)
            }
        }

        nodeLayout.values.forEach { layout ->
            val fillPaint = if (layout.node.isFocus) focusNodePaint else regularNodePaint
            val strokePaint = if (layout.node.isFocus) focusBorderPaint else borderPaint
            canvas.drawRoundRect(layout.rect, cornerRadius, cornerRadius, fillPaint)
            canvas.drawRoundRect(layout.rect, cornerRadius, cornerRadius, strokePaint)
            drawNodeText(canvas, layout)
        }
    }

    private fun drawParentChildLink(canvas: Canvas, link: LineageGraphLink) {
        val parent = nodeLayout[link.fromPersonId] ?: return
        val child = nodeLayout[link.toPersonId] ?: return
        val path = Path().apply {
            moveTo(parent.rect.centerX(), parent.rect.bottom)
            val midY = (parent.rect.bottom + child.rect.top) / 2f
            cubicTo(
                parent.rect.centerX(),
                midY,
                child.rect.centerX(),
                midY,
                child.rect.centerX(),
                child.rect.top,
            )
        }
        canvas.drawPath(path, parentConnectorPaint)
    }

    private fun drawSpouseLink(canvas: Canvas, link: LineageGraphLink) {
        val first = nodeLayout[link.fromPersonId] ?: return
        val second = nodeLayout[link.toPersonId] ?: return
        val sameRow = abs(first.rect.centerY() - second.rect.centerY()) < 1f
        if (sameRow) {
            val leftNode = if (first.rect.left <= second.rect.left) first else second
            val rightNode = if (leftNode === first) second else first
            val y = leftNode.rect.centerY()
            canvas.drawLine(leftNode.rect.right, y, rightNode.rect.left, y, spouseConnectorPaint)
        } else {
            canvas.drawLine(
                first.rect.centerX(),
                first.rect.centerY(),
                second.rect.centerX(),
                second.rect.centerY(),
                spouseConnectorPaint,
            )
        }
    }

    private fun rebuildLayout() {
        val graphValue = graph
        if (graphValue == null || graphValue.nodes.isEmpty()) {
            desiredContentWidth = 0f
            desiredContentHeight = 0f
            nodeLayout = emptyMap()
            return
        }

        val spouseLinks = graphValue.links.filter { it.type == LineageGraphLinkType.SPOUSE }
        val parentChildLinks = graphValue.links.filter { it.type == LineageGraphLinkType.PARENT_CHILD }
        val spouseAdjacency = buildAdjacency(spouseLinks)
        val familyAdjacency = buildAdjacency(parentChildLinks)
        val familyDegree = buildDegreeMap(graphValue.links)
        val generationByNodeId = graphValue.nodes.associate { it.personId to it.generation }

        val rows = graphValue.nodes.groupBy { it.generation }
            .toSortedMap()
            .map { (generation, rowNodes) ->
                RowLayout(
                    generation = generation,
                    clusters = buildClusters(
                        rowNodes = rowNodes,
                        spouseAdjacency = spouseAdjacency,
                        familyDegree = familyDegree,
                    ).toMutableList(),
                )
            }
            .toMutableList()

        optimizeRows(
            rows = rows,
            familyAdjacency = familyAdjacency,
            generationByNodeId = generationByNodeId,
        )

        val widestRowWidth = rows.maxOfOrNull { row ->
            measureRowWidth(row.clusters.sumOf { it.nodes.size })
        } ?: nodeWidth

        desiredContentWidth = widestRowWidth + (contentPadding * 2)
        desiredContentHeight = rows.size.coerceAtLeast(1) * nodeHeight +
            (rows.size - 1).coerceAtLeast(0) * verticalGap +
            (contentPadding * 2)

        val layouts = LinkedHashMap<Long, NodeLayout>()
        rows.forEachIndexed { rowIndex, row ->
            val orderedNodes = row.clusters.flatMap { it.nodes }
            val totalRowWidth = measureRowWidth(orderedNodes.size)
            val startX = contentPadding + ((widestRowWidth - totalRowWidth) / 2f)
            val top = contentPadding + rowIndex * (nodeHeight + verticalGap)

            orderedNodes.forEachIndexed { columnIndex, node ->
                val left = startX + columnIndex * (nodeWidth + horizontalGap)
                layouts[node.personId] = NodeLayout(
                    node = node,
                    rect = RectF(left, top, left + nodeWidth, top + nodeHeight),
                )
            }
        }

        nodeLayout = layouts
    }

    private fun buildClusters(
        rowNodes: List<LineageGraphNode>,
        spouseAdjacency: Map<Long, Set<Long>>,
        familyDegree: Map<Long, Int>,
    ): List<ClusterLayout> {
        val nodesById = rowNodes.associateBy { it.personId }
        val pending = rowNodes.map { it.personId }.toMutableSet()
        val clusters = mutableListOf<ClusterLayout>()

        while (pending.isNotEmpty()) {
            val startId = pending.first()
            val stack = ArrayDeque<Long>()
            val componentIds = mutableListOf<Long>()
            pending.remove(startId)
            stack.add(startId)

            while (stack.isNotEmpty()) {
                val currentId = stack.removeLast()
                componentIds += currentId
                spouseAdjacency[currentId].orEmpty().forEach { spouseId ->
                    if (nodesById.containsKey(spouseId) && pending.remove(spouseId)) {
                        stack.add(spouseId)
                    }
                }
            }

            val componentNodes = componentIds.mapNotNull(nodesById::get)
            clusters += ClusterLayout(
                nodes = arrangeClusterNodes(componentNodes, spouseAdjacency, familyDegree),
            )
        }

        return clusters.sortedWith(
            compareByDescending<ClusterLayout> { cluster -> cluster.nodes.any { it.isFocus } }
                .thenByDescending { cluster -> cluster.nodes.maxOfOrNull { familyDegree[it.personId] ?: 0 } ?: 0 }
                .thenBy { cluster -> cluster.nodes.firstOrNull()?.name?.lowercase().orEmpty() },
        )
    }

    private fun arrangeClusterNodes(
        nodes: List<LineageGraphNode>,
        spouseAdjacency: Map<Long, Set<Long>>,
        familyDegree: Map<Long, Int>,
    ): List<LineageGraphNode> {
        if (nodes.size <= 1) {
            return nodes
        }

        val centerNode = nodes.maxWithOrNull(
            compareBy<LineageGraphNode> { !it.isFocus }
                .thenByDescending { spouseAdjacency[it.personId].orEmpty().size }
                .thenByDescending { familyDegree[it.personId] ?: 0 }
                .thenBy { it.name.lowercase() },
        ) ?: return nodes

        val remaining = nodes
            .filterNot { it.personId == centerNode.personId }
            .sortedWith(
                compareByDescending<LineageGraphNode> { familyDegree[it.personId] ?: 0 }
                    .thenBy { it.name.lowercase() },
            )

        val left = mutableListOf<LineageGraphNode>()
        val right = mutableListOf<LineageGraphNode>()
        remaining.forEachIndexed { index, node ->
            if (index % 2 == 0) {
                left += node
            } else {
                right += node
            }
        }

        return left.asReversed() + centerNode + right
    }

    private fun optimizeRows(
        rows: MutableList<RowLayout>,
        familyAdjacency: Map<Long, Set<Long>>,
        generationByNodeId: Map<Long, Int>,
    ) {
        if (rows.size <= 1) {
            return
        }

        repeat(6) {
            for (index in 1 until rows.size) {
                reorderRowByNeighbor(
                    row = rows[index],
                    referenceGeneration = rows[index - 1].generation,
                    nodeCenters = computeNodeCenters(rows),
                    familyAdjacency = familyAdjacency,
                    generationByNodeId = generationByNodeId,
                )
            }

            for (index in rows.lastIndex - 1 downTo 0) {
                reorderRowByNeighbor(
                    row = rows[index],
                    referenceGeneration = rows[index + 1].generation,
                    nodeCenters = computeNodeCenters(rows),
                    familyAdjacency = familyAdjacency,
                    generationByNodeId = generationByNodeId,
                )
            }
        }
    }

    private fun reorderRowByNeighbor(
        row: RowLayout,
        referenceGeneration: Int,
        nodeCenters: Map<Long, Float>,
        familyAdjacency: Map<Long, Set<Long>>,
        generationByNodeId: Map<Long, Int>,
    ) {
        val rankedClusters = row.clusters.mapIndexed { index, cluster ->
            RankedCluster(
                cluster = cluster,
                barycenter = clusterBarycenter(
                    cluster = cluster,
                    referenceGeneration = referenceGeneration,
                    nodeCenters = nodeCenters,
                    familyAdjacency = familyAdjacency,
                    generationByNodeId = generationByNodeId,
                ) ?: clusterFallbackCenter(cluster, nodeCenters, index),
                stableIndex = index,
            )
        }

        row.clusters.clear()
        row.clusters += rankedClusters.sortedWith(
            compareBy<RankedCluster> { it.barycenter }.thenBy { it.stableIndex },
        ).map { it.cluster }
    }

    private fun clusterBarycenter(
        cluster: ClusterLayout,
        referenceGeneration: Int,
        nodeCenters: Map<Long, Float>,
        familyAdjacency: Map<Long, Set<Long>>,
        generationByNodeId: Map<Long, Int>,
    ): Float? {
        val relatedCenters = buildList {
            cluster.nodes.forEach { node ->
                familyAdjacency[node.personId].orEmpty().forEach { relatedId ->
                    if (generationByNodeId[relatedId] == referenceGeneration) {
                        nodeCenters[relatedId]?.let(::add)
                    }
                }
            }
        }

        if (relatedCenters.isEmpty()) {
            return null
        }

        return relatedCenters.map { it.toDouble() }.average().toFloat()
    }

    private fun clusterFallbackCenter(
        cluster: ClusterLayout,
        nodeCenters: Map<Long, Float>,
        index: Int,
    ): Float {
        val centers = cluster.nodes.mapNotNull { nodeCenters[it.personId] }
        return if (centers.isEmpty()) {
            index.toFloat()
        } else {
            centers.map { it.toDouble() }.average().toFloat()
        }
    }

    private fun computeNodeCenters(rows: List<RowLayout>): Map<Long, Float> {
        val centers = mutableMapOf<Long, Float>()
        rows.forEach { row ->
            val orderedNodes = row.clusters.flatMap { it.nodes }
            orderedNodes.forEachIndexed { index, node ->
                centers[node.personId] = index * (nodeWidth + horizontalGap) + (nodeWidth / 2f)
            }
        }
        return centers
    }

    private fun buildAdjacency(links: List<LineageGraphLink>): Map<Long, Set<Long>> {
        val adjacency = mutableMapOf<Long, MutableSet<Long>>()
        links.forEach { link ->
            adjacency.getOrPut(link.fromPersonId) { linkedSetOf() }.add(link.toPersonId)
            adjacency.getOrPut(link.toPersonId) { linkedSetOf() }.add(link.fromPersonId)
        }
        return adjacency
    }

    private fun buildDegreeMap(links: List<LineageGraphLink>): Map<Long, Int> {
        val degreeMap = mutableMapOf<Long, Int>()
        links.forEach { link ->
            degreeMap[link.fromPersonId] = (degreeMap[link.fromPersonId] ?: 0) + 1
            degreeMap[link.toPersonId] = (degreeMap[link.toPersonId] ?: 0) + 1
        }
        return degreeMap
    }

    private fun measureRowWidth(nodeCount: Int): Float {
        val safeCount = max(nodeCount, 1)
        return safeCount * nodeWidth + (safeCount - 1) * horizontalGap
    }

    private fun drawNodeText(canvas: Canvas, layout: NodeLayout) {
        val textLeft = layout.rect.left + (16f * density)
        val titleBaseline = layout.rect.top + (30f * density)
        val locationBaseline = layout.rect.top + (56f * density)
        val maxTextWidth = nodeWidth - (32f * density)

        val title = TextUtils.ellipsize(
            layout.node.name,
            TextPaint(titlePaint),
            maxTextWidth,
            TextUtils.TruncateAt.END,
        ).toString()
        val location = TextUtils.ellipsize(
            layout.node.location.ifBlank { "Location unavailable" },
            TextPaint(locationPaint),
            maxTextWidth,
            TextUtils.TruncateAt.END,
        ).toString()

        canvas.drawText(title, textLeft, titleBaseline, titlePaint)
        canvas.drawText(location, textLeft, locationBaseline, locationPaint)
    }

    private data class NodeLayout(
        val node: LineageGraphNode,
        val rect: RectF,
    )

    private data class ClusterLayout(
        val nodes: List<LineageGraphNode>,
    )

    private data class RowLayout(
        val generation: Int,
        val clusters: MutableList<ClusterLayout>,
    )

    private data class RankedCluster(
        val cluster: ClusterLayout,
        val barycenter: Float,
        val stableIndex: Int,
    )
}
