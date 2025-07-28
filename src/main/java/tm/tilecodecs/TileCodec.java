/*
*
*    Copyright (C) 2003 Kent Hansen.
*
*    This file is part of Tile Molester.
*
*    Tile Molester is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2 of the License, or
*    (at your option) any later version.
*
*    Tile Molester is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*/

package tm.tilecodecs;

/**
*
* Abstract class for configurable size tile codecs.
* To add a new tile format, simply extend this class and implement decode() and encode().
*
**/

public abstract class TileCodec {

    public static final int MODE_1D=1;
    public static final int MODE_2D=2;

    // Swizzle pattern constants
    public static final String SWIZZLE_NONE = "None";
    public static final String SWIZZLE_BC = "BC";
    public static final String SWIZZLE_PSP = "PSP";
    public static final String SWIZZLE_NDS = "NDS";
    public static final String SWIZZLE_3DS = "3DS";
    public static final String SWIZZLE_WII = "WII";
    public static final String SWIZZLE_SWITCH = "SWITCH";
    public static final String SWIZZLE_CUSTOM = "Custom";

    private String id;
    private String description;
    protected int[] pixels;     // destination for DEcoded tile data
    protected int bitsPerPixel;
    protected int bytesPerRow;  // row = tileWidth pixels
    protected long colorCount;
    protected int tileSize;     // size of one encoded tile
    protected int tileWidth;    // width of tile in pixels
    protected int tileHeight;   // height of tile in pixels
    protected String swizzlePattern;  // current swizzle pattern
    
    // Custom swizzle parameters
    protected int customBlockWidth = 4;   // custom swizzle block width
    protected int customBlockHeight = 4;  // custom swizzle block height
    protected boolean customMortonOrder = true;  // use morton order within blocks

/**
*
* Constructor. Every subclass must call this with argument bitsPerPixel.
*
* @param bitsPerPixel   Duh!
*
**/

    public TileCodec(String id, int bitsPerPixel, String description) {
        this(id, bitsPerPixel, description, 8, 8);
    }

/**
*
* Constructor with configurable tile dimensions.
*
* @param bitsPerPixel   Bits per pixel
* @param tileWidth      Width of tile in pixels
* @param tileHeight     Height of tile in pixels
*
**/

    public TileCodec(String id, int bitsPerPixel, String description, int tileWidth, int tileHeight) {
        this.id = id;
        this.bitsPerPixel = bitsPerPixel;
        this.description = description;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.swizzlePattern = SWIZZLE_NONE;  // default to no swizzling
        bytesPerRow = (bitsPerPixel * tileWidth + 7) / 8; // round up to nearest byte
        tileSize = bytesPerRow * tileHeight;
        colorCount = 1 << bitsPerPixel;
        pixels = new int[tileWidth * tileHeight];
    }

/**
*
* Decodes a tile.
*
* @param bits   An array of encoded tile data
* @param ofs    Start offset of tile in bits array
*
**/

    public abstract int[] decode(byte[] bits, int ofs, int stride);

/**
*
* Encodes a tile.
*
* @param pixels An array of decoded tile data
*
**/

    public abstract void encode(int[] pixels, byte[] bits, int ofs, int stride);

/**
*
* Gets the # of bits per pixel for the tile format.
*
**/

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

/**
*
* Gets the # of bytes per row (tileWidth pixels) for the tile format.
*
**/

    public int getBytesPerRow() {
        return bytesPerRow;
    }

/**
*
* Gets the width of the tile in pixels.
*
**/

    public int getTileWidth() {
        return tileWidth;
    }

/**
*
* Gets the height of the tile in pixels.
*
**/

    public int getTileHeight() {
        return tileHeight;
    }

/**
*
* Sets the tile dimensions and recalculates dependent values.
*
**/

    public void setTileDimensions(int width, int height) {
        this.tileWidth = width;
        this.tileHeight = height;
        bytesPerRow = (bitsPerPixel * tileWidth + 7) / 8; // round up to nearest byte
        tileSize = bytesPerRow * tileHeight;
        pixels = new int[tileWidth * tileHeight];
    }

/**
*
* Gets the current swizzle pattern.
*
**/

    public String getSwizzlePattern() {
        return swizzlePattern;
    }

/**
*
* Sets the swizzle pattern.
*
**/

    public void setSwizzlePattern(String swizzlePattern) {
        this.swizzlePattern = swizzlePattern;
    }

/**
*
* Gets all available swizzle patterns.
*
**/

    public static String[] getAvailableSwizzlePatterns() {
        return new String[] {
            SWIZZLE_NONE,
            SWIZZLE_BC,
            SWIZZLE_PSP,
            SWIZZLE_NDS,
            SWIZZLE_3DS,
            SWIZZLE_WII,
            SWIZZLE_SWITCH,
            SWIZZLE_CUSTOM
        };
    }

/**
*
*
*
**/
/*
    public long getColorCount() {
        return colorCount;
    }
*/
// TEMP!!!!!!!!!!
    public int getColorCount() {
        if (bitsPerPixel < 8) return (1 << bitsPerPixel);
        return 256;
    }

/**
*
* Gets the size in bytes of one tile encoded in this format.
*
**/

    public int getTileSize() {
        return tileSize;
    }

/**
*
* Gets the description of the codec.
*
**/

    public String getDescription() {
        return description;
    }

/**
*
* Gets the codec id.
*
**/

    public String getID() {
        return id;
    }

    public String toString() {
        return description;
    }

    /**
     * Gets the custom swizzle block width.
     */
    public int getCustomBlockWidth() {
        return customBlockWidth;
    }

    /**
     * Sets the custom swizzle block width.
     */
    public void setCustomBlockWidth(int width) {
        this.customBlockWidth = width;
    }

    /**
     * Gets the custom swizzle block height.
     */
    public int getCustomBlockHeight() {
        return customBlockHeight;
    }

    /**
     * Sets the custom swizzle block height.
     */
    public void setCustomBlockHeight(int height) {
        this.customBlockHeight = height;
    }

    /**
     * Gets whether custom swizzle uses Morton order.
     */
    public boolean getCustomMortonOrder() {
        return customMortonOrder;
    }

    /**
     * Sets whether custom swizzle uses Morton order.
     */
    public void setCustomMortonOrder(boolean useMorton) {
        this.customMortonOrder = useMorton;
    }

}