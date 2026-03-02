package com.cake.struts.content.mesh;

import com.cake.struts.content.geometry.StrutVertex;

/**
 * A single clipped edge segment at a strut attachment plane,
 * used by {@link com.cake.struts.content.cap.CapAccumulator} to build end-caps.
 */
public record StrutMeshEdge(StrutVertex start, StrutVertex end) {
}
