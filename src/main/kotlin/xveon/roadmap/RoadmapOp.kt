package xveon.roadmap

data class RoadmapOp(
    var type: RoadmapOpType,
    var oldChunk: RoadmapChunk? = null,
    var newChunk: RoadmapChunk? = null,
    var newMarker: RoadmapMarker? = null,
    var oldMarker: RoadmapMarker? = null,
) { }
