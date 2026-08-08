package net.opanel.api;

import java.util.Objects;

public final class Position {
    private final double x;
    private final double y;
    private final double z;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

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
