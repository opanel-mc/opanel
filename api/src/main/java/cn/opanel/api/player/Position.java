package cn.opanel.api.player;

import java.util.Objects;

/**
 * Immutable three-dimensional player position in Minecraft block coordinates.
 */
public final class Position {
    private final double x;
    private final double y;
    private final double z;

    /**
     * Creates a position value.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** @return the X coordinate */
    public double getX() {
        return x;
    }

    /** @return the Y coordinate */
    public double getY() {
        return y;
    }

    /** @return the Z coordinate */
    public double getZ() {
        return z;
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) return true;
        if(!(object instanceof Position)) return false;
        Position position = (Position) object;
        return Double.compare(x, position.x) == 0
                && Double.compare(y, position.y) == 0
                && Double.compare(z, position.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Position{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
