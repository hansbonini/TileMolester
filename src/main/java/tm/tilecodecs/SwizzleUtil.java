/*
*
*    Copyright (C) 2024 Hans Bonini.
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
* Utility class for swizzle/deswizzle operations.
* Supports various platform-specific swizzle patterns with optimized implementations.
* 
* Each swizzle pattern is designed to work with specific tile dimensions for optimal results:
* - BC, WII, SWITCH: 4x4 tiles (block-based formats)
* - NDS, 3DS: 8x8 tiles (traditional tile formats)  
* - PSP: Variable dimensions (morton-based)
* - Custom: User-configurable
*
**/

public class SwizzleUtil {
    
    // Platform-specific optimal tile dimensions
    private static final int BLOCK_FORMAT_TILE_SIZE = 4;  // BC, WII, SWITCH
    private static final int NINTENDO_TILE_SIZE = 8;      // NDS, 3DS

    /**
     * Applies swizzling to pixel coordinates based on the specified pattern.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Tile width
     * @param height Tile height
     * @param pattern Swizzle pattern
     * @return Swizzled linear index
     */
    public static int applySwizzle(int x, int y, int width, int height, String pattern) {
        if (TileCodec.SWIZZLE_NONE.equals(pattern)) {
            return y * width + x;
        }
        
        switch (pattern) {
            case TileCodec.SWIZZLE_BC:
                return applyBCSwizzle(x, y, width, height);
            case TileCodec.SWIZZLE_PSP:
                return applyPSPSwizzle(x, y, width, height);
            case TileCodec.SWIZZLE_NDS:
                return applyNDSSwizzle(x, y, width, height);
            case TileCodec.SWIZZLE_3DS:
                return apply3DSSwizzle(x, y, width, height);
            case TileCodec.SWIZZLE_WII:
                return applyWiiSwizzle(x, y, width, height);
            case TileCodec.SWIZZLE_SWITCH:
                return applySwitchSwizzle(x, y, width, height);
            case TileCodec.SWIZZLE_CUSTOM:
                return applyCustomSwizzle(x, y, width, height, 4, 4, true); // Default custom values
            default:
                return y * width + x; // No swizzling
        }
    }

    /**
     * Sets optimal tile dimensions for a given swizzle pattern.
     * 
     * @param pattern Swizzle pattern
     * @param codec TileCodec to update
     */
    public static void setOptimalTileDimensions(String pattern, TileCodec codec) {
        if (codec == null) return;
        
        switch (pattern) {
            case TileCodec.SWIZZLE_SWITCH:
            case TileCodec.SWIZZLE_BC:
            case TileCodec.SWIZZLE_WII:
                // Block-based formats work best with 4x4 tile dimensions
                codec.setTileDimensions(BLOCK_FORMAT_TILE_SIZE, BLOCK_FORMAT_TILE_SIZE);
                break;
            case TileCodec.SWIZZLE_NDS:
            case TileCodec.SWIZZLE_3DS:
                // Nintendo handheld formats typically use 8x8 tiles
                codec.setTileDimensions(NINTENDO_TILE_SIZE, NINTENDO_TILE_SIZE);
                break;
            // PSP and Custom don't change tile dimensions automatically
            case TileCodec.SWIZZLE_PSP:
            case TileCodec.SWIZZLE_CUSTOM:
            case TileCodec.SWIZZLE_NONE:
            default:
                // Keep existing dimensions
                break;
        }
    }

    /**
     * Applies swizzling with custom parameters.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Tile width
     * @param height Tile height
     * @param pattern Swizzle pattern
     * @param customBlockWidth Custom block width (for custom pattern)
     * @param customBlockHeight Custom block height (for custom pattern)
     * @param customMortonOrder Whether to use Morton order (for custom pattern)
     * @return Swizzled linear index
     */
    public static int applySwizzle(int x, int y, int width, int height, String pattern, 
                                 int customBlockWidth, int customBlockHeight, boolean customMortonOrder) {
        if (TileCodec.SWIZZLE_CUSTOM.equals(pattern)) {
            return applyCustomSwizzle(x, y, width, height, customBlockWidth, customBlockHeight, customMortonOrder);
        } else {
            return applySwizzle(x, y, width, height, pattern);
        }
    }

    /**
     * Reverses swizzling to get original coordinates.
     * 
     * @param index Linear index
     * @param width Tile width
     * @param height Tile height
     * @param pattern Swizzle pattern
     * @return Array containing {x, y} coordinates
     */
    public static int[] reverseSwizzle(int index, int width, int height, String pattern) {
        if (TileCodec.SWIZZLE_NONE.equals(pattern)) {
            return new int[] { index % width, index / width };
        }
        
        // For reverse operations, we need to find which x,y produces the given index
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (applySwizzle(x, y, width, height, pattern) == index) {
                    return new int[] { x, y };
                }
            }
        }
        
        // Fallback to linear
        return new int[] { index % width, index / width };
    }

    /**
     * Block Compression (BC) swizzle pattern.
     * Used by GPU texture compression formats like BC1-BC7.
     * Optimized for 4x4 tile dimensions.
     */
    private static int applyBCSwizzle(int x, int y, int width, int height) {
        // BC formats use 4x4 blocks
        final int blockSize = BLOCK_FORMAT_TILE_SIZE;
        
        int blockX = x / blockSize;
        int blockY = y / blockSize;
        int inBlockX = x % blockSize;
        int inBlockY = y % blockSize;
        
        int blocksPerRow = (width + blockSize - 1) / blockSize;
        int blockIndex = blockY * blocksPerRow + blockX;
        int pixelInBlock = inBlockY * blockSize + inBlockX;
        
        return blockIndex * (blockSize * blockSize) + pixelInBlock;
    }

    /**
     * PSP swizzle pattern.
     * PSP uses Morton order (Z-order curve) for better cache performance.
     * Works with variable tile dimensions.
     */
    private static int applyPSPSwizzle(int x, int y, int width, int height) {
        // PSP uses pure Morton order - interleave bits of x and y coordinates
        return mortonEncode2D(x, y);
    }

    /**
     * Nintendo DS swizzle pattern.
     * NDS uses 8x8 tiles with Morton order within each tile.
     * Optimized for 8x8 tile dimensions.
     */
    private static int applyNDSSwizzle(int x, int y, int width, int height) {
        final int tileSize = NINTENDO_TILE_SIZE;
        
        // Calculate which 8x8 tile this pixel belongs to
        int tileX = x / tileSize;
        int tileY = y / tileSize;
        int inTileX = x % tileSize;
        int inTileY = y % tileSize;
        
        int tilesPerRow = (width + tileSize - 1) / tileSize;
        int tileIndex = tileY * tilesPerRow + tileX;
        int pixelInTile = mortonEncode2D(inTileX, inTileY);
        
        return tileIndex * (tileSize * tileSize) + pixelInTile;
    }

    /**
     * Nintendo 3DS swizzle pattern.
     * 3DS uses 8x8 tiles with Morton order, similar to NDS but with enhanced bit arrangement.
     * Optimized for 8x8 tile dimensions.
     */
    private static int apply3DSSwizzle(int x, int y, int width, int height) {
        final int tileSize = NINTENDO_TILE_SIZE;
        
        // Calculate which 8x8 tile this pixel belongs to
        int tileX = x / tileSize;
        int tileY = y / tileSize;
        int inTileX = x % tileSize;
        int inTileY = y % tileSize;
        
        int tilesPerRow = (width + tileSize - 1) / tileSize;
        int tileIndex = tileY * tilesPerRow + tileX;
        int pixelInTile = mortonEncode2D(inTileX, inTileY);
        
        return tileIndex * (tileSize * tileSize) + pixelInTile;
    }

    /**
     * Nintendo Switch swizzle pattern.
     * Switch uses block-linear format optimized for GPU memory access.
     * Implements a simplified version of the Switch's GOB (Group of Blocks) structure.
     * Optimized for 4x4 tile dimensions.
     */
    private static int applySwitchSwizzle(int x, int y, int width, int height) {
        final int blockSize = BLOCK_FORMAT_TILE_SIZE;
        
        // Calculate block coordinates
        int blockX = x / blockSize;
        int blockY = y / blockSize;
        int pixelX = x % blockSize;
        int pixelY = y % blockSize;
        
        // Switch uses GOBs (Groups of Blocks) for memory layout optimization
        // For simplicity, we'll use 8x2 blocks per GOB (32x8 pixels) which works well for most formats
        final int gobWidthInBlocks = 8;
        final int gobHeightInBlocks = 2;
        
        int blocksPerRow = (width + blockSize - 1) / blockSize;
        
        // GOB coordinates
        int gobX = blockX / gobWidthInBlocks;
        int gobY = blockY / gobHeightInBlocks;
        int gobsPerRow = (blocksPerRow + gobWidthInBlocks - 1) / gobWidthInBlocks;
        
        // Block position within GOB
        int blockInGobX = blockX % gobWidthInBlocks;
        int blockInGobY = blockY % gobHeightInBlocks;
        
        // Switch uses a specific block ordering within GOBs for optimal memory access
        int blockInGobIndex = blockInGobY * gobWidthInBlocks + blockInGobX;
        
        // Pixel position within block (linear order)
        int pixelInBlock = pixelY * blockSize + pixelX;
        
        // Calculate final position
        int gobIndex = gobY * gobsPerRow + gobX;
        int pixelsPerGob = gobWidthInBlocks * gobHeightInBlocks * blockSize * blockSize;
        int pixelsPerBlock = blockSize * blockSize;
        
        return gobIndex * pixelsPerGob + blockInGobIndex * pixelsPerBlock + pixelInBlock;
    }

    /**
     * Nintendo Wii swizzle pattern.
     * Uses 4x4 blocks arranged in 2x2 super-blocks for optimal cache performance.
     * Optimized for 4x4 tile dimensions.
     */
    private static int applyWiiSwizzle(int x, int y, int width, int height) {
        final int blockSize = BLOCK_FORMAT_TILE_SIZE;
        
        // Calculate which 4x4 block this pixel belongs to
        int blockX = x / blockSize;
        int blockY = y / blockSize;
        int inBlockX = x % blockSize;
        int inBlockY = y % blockSize;
        
        int blocksPerRow = (width + blockSize - 1) / blockSize;
        
        // Wii arranges 4x4 blocks in 2x2 super-blocks for better cache locality
        int superBlockX = blockX / 2;
        int superBlockY = blockY / 2;
        int blockInSuperX = blockX % 2;
        int blockInSuperY = blockY % 2;
        
        int superBlocksPerRow = (blocksPerRow + 1) / 2;
        int superBlockIndex = superBlockY * superBlocksPerRow + superBlockX;
        
        // Within a super-block, blocks are arranged as:
        // 0 1
        // 2 3
        int blockInSuper = blockInSuperY * 2 + blockInSuperX;
        
        // Within a 4x4 block, pixels are stored linearly
        int pixelInBlock = inBlockY * blockSize + inBlockX;
        
        return superBlockIndex * (4 * blockSize * blockSize) + blockInSuper * (blockSize * blockSize) + pixelInBlock;
    }

    /**
     * Custom swizzle pattern with configurable parameters.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Tile width
     * @param height Tile height
     * @param blockWidth Block width
     * @param blockHeight Block height
     * @param useMortonOrder Whether to use Morton order within blocks
     * @return Swizzled linear index
     */
    private static int applyCustomSwizzle(int x, int y, int width, int height, 
                                        int blockWidth, int blockHeight, boolean useMortonOrder) {
        // Calculate which block this pixel belongs to
        int blockX = x / blockWidth;
        int blockY = y / blockHeight;
        int inBlockX = x % blockWidth;
        int inBlockY = y % blockHeight;
        
        int blocksPerRow = (width + blockWidth - 1) / blockWidth;
        int blockIndex = blockY * blocksPerRow + blockX;
        
        int pixelInBlock;
        if (useMortonOrder) {
            // Use Morton order (Z-order) within the block
            pixelInBlock = mortonEncode2D(inBlockX, inBlockY);
        } else {
            // Use linear order within the block
            pixelInBlock = inBlockY * blockWidth + inBlockX;
        }
        
        return blockIndex * (blockWidth * blockHeight) + pixelInBlock;
    }

    /**
     * Morton encoding (Z-order curve) for 2D coordinates.
     * Efficiently interleaves bits of x and y coordinates using bit manipulation.
     * 
     * @param x X coordinate (must be < 65536 for 16-bit processing)
     * @param y Y coordinate (must be < 65536 for 16-bit processing)
     * @return Morton-encoded value
     */
    private static int mortonEncode2D(int x, int y) {
        // Use efficient bit interleaving for coordinates up to 16 bits
        x = (x | (x << 8)) & 0x00FF00FF;
        x = (x | (x << 4)) & 0x0F0F0F0F;
        x = (x | (x << 2)) & 0x33333333;
        x = (x | (x << 1)) & 0x55555555;
        
        y = (y | (y << 8)) & 0x00FF00FF;
        y = (y | (y << 4)) & 0x0F0F0F0F;
        y = (y | (y << 2)) & 0x33333333;
        y = (y | (y << 1)) & 0x55555555;
        
        return x | (y << 1);
    }
    
    /**
     * Gets the optimal tile width for a given swizzle pattern.
     * 
     * @param pattern Swizzle pattern
     * @return Optimal tile width, or -1 if no specific recommendation
     */
    public static int getOptimalTileWidth(String pattern) {
        switch (pattern) {
            case TileCodec.SWIZZLE_SWITCH:
            case TileCodec.SWIZZLE_BC:
            case TileCodec.SWIZZLE_WII:
                return BLOCK_FORMAT_TILE_SIZE;
            case TileCodec.SWIZZLE_NDS:
            case TileCodec.SWIZZLE_3DS:
                return NINTENDO_TILE_SIZE;
            default:
                return -1; // No specific recommendation
        }
    }
    
    /**
     * Gets the optimal tile height for a given swizzle pattern.
     * 
     * @param pattern Swizzle pattern
     * @return Optimal tile height, or -1 if no specific recommendation
     */
    public static int getOptimalTileHeight(String pattern) {
        return getOptimalTileWidth(pattern); // All current patterns use square tiles
    }
    
    /**
     * Checks if the given tile dimensions are optimal for the swizzle pattern.
     * 
     * @param pattern Swizzle pattern
     * @param width Current tile width
     * @param height Current tile height
     * @return true if dimensions are optimal, false otherwise
     */
    public static boolean areOptimalDimensions(String pattern, int width, int height) {
        int optimalWidth = getOptimalTileWidth(pattern);
        int optimalHeight = getOptimalTileHeight(pattern);
        
        if (optimalWidth == -1 || optimalHeight == -1) {
            return true; // No specific requirements
        }
        
        return width == optimalWidth && height == optimalHeight;
    }
}
