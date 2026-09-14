package me.jamino.analogairwaves.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The blockstate rotates the model with "y": 90 per facing step, which maps a model point
 * (x, z) to (16 - z, x). The outline must follow the same mapping or it drifts away from the
 * radio for every facing but north.
 */
class ShapeRotationTest {
    /** The portable radio's aerial in the north-facing model: x 11-12, y 7-15, z 7-8. */
    private static final VoxelShape AERIAL = px(11, 7, 7, 12, 15, 8);

    @Test
    void rotatesTheAerialTheSameWayTheBlockstateRotatesTheModel() {
        AABB east = ShapeRotation.rotateClockwise(AERIAL).toAabbs().getFirst();

        // Facing east the aerial must land at x 8-9, z 11-12, same height.
        assertBox(east, 8, 7, 11, 9, 15, 12);
    }

    @Test
    void mapsEachFacingToTheMatchingRotation() {
        Map<Direction, VoxelShape> shapes = ShapeRotation.horizontal(AERIAL);

        assertBox(shapes.get(Direction.NORTH).toAabbs().getFirst(), 11, 7, 7, 12, 15, 8);
        assertBox(shapes.get(Direction.EAST).toAabbs().getFirst(), 8, 7, 11, 9, 15, 12);
        assertBox(shapes.get(Direction.SOUTH).toAabbs().getFirst(), 4, 7, 8, 5, 15, 9);
        assertBox(shapes.get(Direction.WEST).toAabbs().getFirst(), 7, 7, 4, 8, 15, 5);
    }

    @Test
    void fourRotationsReturnToTheStartingShape() {
        VoxelShape start = px(3, 0, 5, 13, 7, 11);
        VoxelShape shape = start;
        for (int i = 0; i < 4; i++) {
            shape = ShapeRotation.rotateClockwise(shape);
        }
        assertEquals(start.toAabbs(), shape.toAabbs());
    }

    private static VoxelShape px(double x0, double y0, double z0, double x1, double y1, double z1) {
        return Shapes.box(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
    }

    private static void assertBox(AABB box, double x0, double y0, double z0, double x1, double y1, double z1) {
        assertEquals(x0 / 16, box.minX, 1e-9);
        assertEquals(y0 / 16, box.minY, 1e-9);
        assertEquals(z0 / 16, box.minZ, 1e-9);
        assertEquals(x1 / 16, box.maxX, 1e-9);
        assertEquals(y1 / 16, box.maxY, 1e-9);
        assertEquals(z1 / 16, box.maxZ, 1e-9);
    }
}
