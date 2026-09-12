package me.jamino.analog_airwaves.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Turns a north-facing outline to match a model rotated by the blockstate's {@code "y"}
 * rotation. A blockstate {@code "y": 90} maps a model point (x, z) to (16 - z, x), and so
 * does {@link #rotateClockwise}; applying it once per facing step keeps the outline on the model.
 */
public final class ShapeRotation {
    /** The four horizontal facings in blockstate rotation order: north, then +90° each. */
    private static final Direction[] ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static Map<Direction, VoxelShape> horizontal(VoxelShape northShape) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        VoxelShape shape = northShape;
        for (Direction facing : ORDER) {
            shapes.put(facing, shape);
            shape = rotateClockwise(shape);
        }
        return shapes;
    }

    public static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape rotated = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            rotated = Shapes.or(rotated,
                    Shapes.box(1.0D - box.maxZ, box.minY, box.minX, 1.0D - box.minZ, box.maxY, box.maxX));
        }
        return rotated;
    }

    private ShapeRotation() {
    }
}
