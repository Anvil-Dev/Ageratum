package dev.anvilcraft.resource.ageratum.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureGuiGeometryTest {
    @Test
    void connectorStopsAtWindowEdgeOnTheCenterRay() {
        for (float[] anchor : new float[][]{{400, 120}, {0, 120}, {180, -100}, {180, 400}, {400, 400}}) {
            var edge = StructureGuiGeometry.edge(100, 80, 160, 80, anchor[0], anchor[1]);
            assertTrue(edge.x >= 100 && edge.x <= 260 && edge.y >= 80 && edge.y <= 160);
            assertTrue(edge.x == 100 || edge.x == 260 || edge.y == 80 || edge.y == 160);
            assertEquals(0, (edge.x - 180) * (anchor[1] - 120) - (edge.y - 120) * (anchor[0] - 180), 0.002);
        }
    }

    @Test
    void connectorDoesNotCrossWindowInterior() {
        var edge = StructureGuiGeometry.edge(100, 80, 160, 80, 150, 100);
        assertEquals(150, edge.x);
        assertEquals(100, edge.y);
    }

    @Test
    void transitionIsMonotonicAndClampsOutsideItsDuration() {
        float previous = StructureGuiGeometry.easeOut(0);
        for (int frame = 1; frame <= 100; frame++) {
            float next = StructureGuiGeometry.easeOut(frame / 100f);
            assertTrue(next >= previous && next <= 1);
            previous = next;
        }
        assertEquals(0, StructureGuiGeometry.easeOut(-1), 0.0001);
        assertEquals(1, StructureGuiGeometry.easeOut(2), 0.0001);
    }
}
