package cn.opanel.api.player;

import java.util.Objects;

/**
 * Immutable three-dimensional player position in Minecraft block coordinates.
 *
 * <p>以 Minecraft 方块坐标表示的不可变三维玩家位置。</p>
 */
public final class Position { // 以 Minecraft 方块坐标表示的不可变三维玩家位置
    private final double x; // X 坐标
    private final double y; // Y 坐标
    private final double z; // Z 坐标

    /**
     * Creates a position value.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public Position(double x, double y, double z) { // 创建位置值
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** @return the X coordinate */
    public double getX() { // 返回 X 坐标
        return x;
    }

    /** @return the Y coordinate */
    public double getY() { // 返回 Y 坐标
        return y;
    }

    /** @return the Z coordinate */
    public double getZ() { // 返回 Z 坐标
        return z;
    }

    @Override
    public boolean equals(Object object) { // 按 X、Y、Z 坐标比较两个位置
        if(this == object) return true;
        if(!(object instanceof Position)) return false;
        Position position = (Position) object;
        return Double.compare(x, position.x) == 0
                && Double.compare(y, position.y) == 0
                && Double.compare(z, position.z) == 0;
    }

    @Override
    public int hashCode() { // 基于 X、Y、Z 坐标计算哈希码
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() { // 返回包含 X、Y、Z 坐标的调试字符串
        return "Position{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
