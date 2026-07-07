package net.opanel.utils;

public class AnvilUtility {
    private static final int DEFAULT_HEIGHT_BITS = 9;

    public static int[] bitunpack(long[] packed, int bitsPerValue) {
        final int valueAmount = Long.SIZE / bitsPerValue;
        final long mask = (1L << bitsPerValue) - 1;

        int[] values = new int[valueAmount * packed.length];
        for(int i = 0; i < packed.length; i++) {
            long packedLong = packed[i];
            for(int j = 0; j < valueAmount; j++) {
                values[i * valueAmount + j] = (int) (packedLong & mask);
                packedLong >>>= bitsPerValue;
            }
        }
        return values;
    }

    public static long[] bitpack(int[] values, int bitsPerValue) {
        final int valueAmount = Long.SIZE / bitsPerValue;
        final long mask = (1L << bitsPerValue) - 1;

        long[] packed = new long[(values.length + valueAmount - 1) / valueAmount];
        long packedLong = 0L;
        int packedAmount = 0;
        for(int i = 0; i < values.length; i++) {
            long transformed = (values[i] & mask) << (packedAmount * bitsPerValue);
            packedLong |= transformed;
            packedAmount++;
            if(packedAmount >= valueAmount) {
                packed[i / valueAmount] = packedLong;
                packedLong = 0L;
                packedAmount = 0;
            }
        }
        if(packedAmount > 0) {
            packed[packed.length - 1] = packedLong;
        }
        return packed;
    }

    public static int paletteSizeToBitsSize(int paletteSize) {
        return paletteSizeToBitsSize(paletteSize, 1);
    }

    public static int paletteSizeToBitsSize(int paletteSize, int minSize) {
        // equals to Math.max(minSize, Math.ceil(Math.log(paletteSize) / Math.log(2))
        return paletteSize <= 1 ? minSize : Math.max(minSize, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
    }

    public static int heightMapBitsFromSectionCount(int sectionCount) {
        if(sectionCount <= 0) return DEFAULT_HEIGHT_BITS;
        return paletteSizeToBitsSize(sectionCount * 16 + 1);
    }

    public static int maxStoredHeightFromSectionCount(int sectionCount) {
        int bits = heightMapBitsFromSectionCount(sectionCount);
        return (1 << bits) - 1;
    }

    public static int[] getGlobalChunkPosition(String mcaFileName, int chunkX, int chunkZ) throws NumberFormatException {
        String[] parts = mcaFileName.split("\\.");
        int mcaX = Integer.parseInt(parts[1]);
        int mcaZ = Integer.parseInt(parts[2]);

        return new int[] {
            mcaX * 32 + chunkX,
            mcaZ * 32 + chunkZ
        };
    }
}
