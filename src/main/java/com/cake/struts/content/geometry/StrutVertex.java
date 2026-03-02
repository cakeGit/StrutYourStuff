package com.cake.struts.content.geometry;

import org.joml.Vector3f;

public record StrutVertex(Vector3f position, Vector3f normal, float u, float v, int color, int light) {
}

