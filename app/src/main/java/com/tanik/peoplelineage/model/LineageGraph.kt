package com.tanik.peoplelineage.model

data class LineageGraph(
    val focusPersonId: Long,
    val nodes: List<LineageGraphNode>,
    val links: List<LineageGraphLink>,
)

data class LineageGraphNode(
    val personId: Long,
    val name: String,
    val location: String,
    val generation: Int,
    val isFocus: Boolean,
)

data class LineageGraphLink(
    val fromPersonId: Long,
    val toPersonId: Long,
    val type: LineageGraphLinkType,
)

enum class LineageGraphLinkType {
    PARENT_CHILD,
    SPOUSE,
}
