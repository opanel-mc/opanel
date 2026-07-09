package net.opanel.utils;

public class AnvilUtility {
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

    /**
     * Calculate the bit width required by a heightmap for the given world height range.
     *
     * @param minY inclusive lower world height bound
     * @param maxY exclusive upper world height bound, so the highest valid block Y is {@code maxY - 1}
     * @return bits required to store heightmap values relative to {@code minY}, including the zero/empty slot
     */
    public static int heightMapBitsFromYRange(int minY, int maxY) {
        if(maxY <= minY) {
            throw new IllegalArgumentException("maxY must be greater than minY");
        }

        return paletteSizeToBitsSize(maxY - minY + 1);
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
