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
* Planar, palette-indexed 8x8 tile codec. Max. 8 bitplanes.
* bitsPerPixel must be a power of 2. (1, 2, 4, 8) (why??)
* Planes for each row must be stored sequentially.
*
**/

public class PlanarTileCodec extends TileCodec {

    protected int[] bpOffsets;
    protected int[] bp;
    protected static int[][][] bitsToPixelsLookup=null;

/**
*
* Constructor.
*
* @param bpOffsets  Relative offsets for the bitplane values in a row (8 pixels) of encoded tile data. The length of this array is the number of bitplanes in a tile row, which is equal to the # of bits per pixel.
*
**/

    public PlanarTileCodec(String id, int[] bpOffsets, String description) {
        super(id, bpOffsets.length, description);
        this.bpOffsets = bpOffsets;
        bp = new int[8];

        if (bitsToPixelsLookup == null) {
            // Precalculate all bit patterns
            bitsToPixelsLookup = new int[8][256][8];
            for (int i=0; i<8; i++) {
                // do one bitplane
                for (int j=0; j<256; j++) {
                    // do one byte
                    for (int k=7; k>=0; k--) {
                        // do one pixel
                        bitsToPixelsLookup[i][j][7-k] = ((j >> k) & 1) << i;
                    }
                }
            }
        }
    }

/**
*
* Decodes a tile.
*
* @param bits   An array of ints holding encoded tile data in each LSB
* @param ofs    Where to start decoding from in the array
*
**/

    public int[] decode(byte[] bits, int ofs, int stride) {
        int[] pixels = new int[tileWidth * tileHeight];
        int pos = 0;
        stride++;
        stride *= bytesPerRow;
        
        for (int i = 0; i < tileHeight; i++) {
            // Process pixels in 8-pixel blocks for this row
            int pixelsProcessed = 0;
            int rowOfs = ofs;
            
            while (pixelsProcessed < tileWidth) {
                // Read bitplanes for this 8-pixel block
                for (int j = 0; j < bitsPerPixel; j++) {
                    if (rowOfs + bpOffsets[j] < bits.length) {
                        bp[j] = bits[rowOfs + bpOffsets[j]] & 0xFF;
                    } else {
                        bp[j] = 0;
                    }
                }
                
                // Decode up to 8 pixels from this block
                int pixelsInThisBlock = Math.min(8, tileWidth - pixelsProcessed);
                for (int j = 0; j < pixelsInThisBlock; j++) {
                    int p = 0;
                    for (int k = 0; k < bitsPerPixel; k++) {
                        p |= bitsToPixelsLookup[k][bp[k]][j];
                    }
                    if (pos < pixels.length) {
                        pixels[pos++] = p;
                    }
                }
                
                pixelsProcessed += pixelsInThisBlock;
                
                // Move to next 8-pixel block in the same row
                rowOfs += bytesPerRow;
            }
            
            // Move to next row
            ofs += stride;
        }
        return pixels;
    }

/**
*
* Encodes a bitplaned tile.
*
**/

    public void encode(int[] pixels, byte[] bits, int ofs, int stride) {
        int pos = 0;
        stride++;
        stride *= bytesPerRow;
        
        for (int i = 0; i < tileHeight; i++) {
            // Process pixels in 8-pixel blocks for this row
            int pixelsProcessed = 0;
            int rowOfs = ofs;
            
            while (pixelsProcessed < tileWidth) {
                // Reset bitplanes for this 8-pixel block
                for (int j = 0; j < bitsPerPixel; j++) {
                    if (rowOfs + bpOffsets[j] < bits.length) {
                        bits[rowOfs + bpOffsets[j]] = 0;
                    }
                }
                
                // Encode up to 8 pixels in this block
                int pixelsInThisBlock = Math.min(8, tileWidth - pixelsProcessed);
                for (int j = 0; j < pixelsInThisBlock; j++) {
                    int p = (pos < pixels.length) ? pixels[pos++] : 0;
                    
                    for (int k = 0; k < bitsPerPixel; k++) {
                        if (rowOfs + bpOffsets[k] < bits.length) {
                            bits[rowOfs + bpOffsets[k]] |= ((p >> k) & 0x01) << (7 - j);
                        }
                    }
                }
                
                pixelsProcessed += pixelsInThisBlock;
                
                // Move to next 8-pixel block in the same row
                rowOfs += bytesPerRow;
            }
            
            // Move to next row
            ofs += stride;
        }
    }

}