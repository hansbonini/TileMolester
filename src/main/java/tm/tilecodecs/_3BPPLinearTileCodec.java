package tm.tilecodecs;

public class _3BPPLinearTileCodec extends TileCodec {

/**
* Constructor.
**/

    public _3BPPLinearTileCodec() {
        super("LN98", 3, "3bpp linear");
    }

/**
*
* Decodes a tile.
*
**/

    public int[] decode(byte[] bits, int ofs, int stride) {
        int[] pixels = new int[tileWidth * tileHeight];
        int pos=0;
        int b1, b2, b3;
        stride *= bytesPerRow;
        for (int i=0; i<tileHeight; i++) {
            // do one row
            b1 = bits[ofs++] & 0xFF; // byte 1: 0001 1122
            b2 = bits[ofs++] & 0xFF; // byte 2: 2333 4445
            b3 = bits[ofs++] & 0xFF; // byte 3: 5566 6777
            
            // Original 8-pixel decoding with bounds checking
            if (pos + 7 < pixels.length) {
                pixels[pos++] = (b1 >> 5) & 7;
                pixels[pos++] = (b1 >> 2) & 7;
                pixels[pos++] = ((b1 & 3) << 1) | ((b2 >> 7) & 1);
                pixels[pos++] = (b2 >> 4) & 7;
                pixels[pos++] = (b2 >> 1) & 7;
                pixels[pos++] = ((b2 & 1) << 2) | ((b3 >> 6) & 3);
                pixels[pos++] = (b3 >> 3) & 7;
                pixels[pos++] = b3 & 7;
            } else {
                // Handle tiles narrower than 8 pixels
                for (int j = 0; j < 8 && pos < pixels.length; j++) {
                    switch (j) {
                        case 0: pixels[pos++] = (b1 >> 5) & 7; break;
                        case 1: pixels[pos++] = (b1 >> 2) & 7; break;
                        case 2: pixels[pos++] = ((b1 & 3) << 1) | ((b2 >> 7) & 1); break;
                        case 3: pixels[pos++] = (b2 >> 4) & 7; break;
                        case 4: pixels[pos++] = (b2 >> 1) & 7; break;
                        case 5: pixels[pos++] = ((b2 & 1) << 2) | ((b3 >> 6) & 3); break;
                        case 6: pixels[pos++] = (b3 >> 3) & 7; break;
                        case 7: pixels[pos++] = b3 & 7; break;
                    }
                }
            }
            ofs += stride;
        }
        return pixels;
    }

/**
*
* Encodes a tile.
*
**/

    public void encode(int[] pixels, byte[] bits, int ofs, int stride) {
        int pos = 0;
        int b1, b2, b3;
        stride *= bytesPerRow;
        for (int i=0; i<tileHeight; i++) {
            // do one row
            if (pos + 7 < pixels.length) {
                // Standard 8-pixel encoding
                b1 = (pixels[pos++] & 7) << 5;
                b1 |= (pixels[pos++] & 7) << 2;
                b1 |= (pixels[pos] & 6) >> 1;
                b2 = (pixels[pos++] & 1) << 7;
                b2 |= (pixels[pos++] & 7) << 4;
                b2 |= (pixels[pos++] & 7) << 1;
                b2 |= (pixels[pos] & 4) >> 2;
                b3 = (pixels[pos++] & 3) << 6;
                b3 |= (pixels[pos++] & 7) << 3;
                b3 |= (pixels[pos++] & 7);
            } else {
                // Handle tiles narrower than 8 pixels
                b1 = b2 = b3 = 0;
                int[] rowPixels = new int[8];
                for (int j = 0; j < 8; j++) {
                    rowPixels[j] = (pos < pixels.length) ? pixels[pos++] : 0;
                }
                
                b1 = (rowPixels[0] & 7) << 5;
                b1 |= (rowPixels[1] & 7) << 2;
                b1 |= (rowPixels[2] & 6) >> 1;
                b2 = (rowPixels[2] & 1) << 7;
                b2 |= (rowPixels[3] & 7) << 4;
                b2 |= (rowPixels[4] & 7) << 1;
                b2 |= (rowPixels[5] & 4) >> 2;
                b3 = (rowPixels[5] & 3) << 6;
                b3 |= (rowPixels[6] & 7) << 3;
                b3 |= (rowPixels[7] & 7);
            }
            
            bits[ofs++] = (byte)b1; // byte 1: 0001 1122
            bits[ofs++] = (byte)b2; // byte 2: 2333 4445
            bits[ofs++] = (byte)b3; // byte 3: 5566 6777
            ofs += stride;
        }
    }

}