package tm.tilecodecs;

public class _6BPPLinearTileCodec extends TileCodec {

/**
* Constructor.
**/

    public _6BPPLinearTileCodec() {
        super("LN99", 6, "6bpp linear, reverse-order");
    }

/**
*
* Decodes a tile.
*
**/

    public int[] decode(byte[] bits, int ofs, int stride) {
        int[] pixels = new int[tileWidth * tileHeight];
        int pos=0;
        int b1, b2, b3, b4, b5, b6;
        stride *= bytesPerRow;
        for (int i=0; i<tileHeight; i++) {
            // Process pixels for each row
            int pixelsProcessed = 0;
            
            // Process 8-pixel blocks from the data
            while (pixelsProcessed < tileWidth) {
                // Read 6 bytes that encode 8 pixels
                if (ofs + 5 < bits.length) {
                    b6 = bits[ofs++] & 0xFF; // byte 1: 0000 0011
                    b5 = bits[ofs++] & 0xFF; // byte 2: 1111 2222
                    b4 = bits[ofs++] & 0xFF; // byte 3: 2233 3333
                    b3 = bits[ofs++] & 0xFF; // byte 4: 4444 4455
                    b2 = bits[ofs++] & 0xFF; // byte 5: 5555 6666
                    b1 = bits[ofs++] & 0xFF; // byte 6: 6677 7777
                    
                    // Decode up to 8 pixels, but only use what we need
                    int[] decodedPixels = new int[8];
                    decodedPixels[0] = (b1 >> 2) & 63;
                    decodedPixels[1] = ((b1 & 3) << 4) | ((b2 >> 4) & 15);
                    decodedPixels[2] = ((b2 & 15) << 2) | ((b3 >> 6) & 3);
                    decodedPixels[3] = b3 & 63;
                    decodedPixels[4] = (b4 >> 2) & 63;
                    decodedPixels[5] = ((b4 & 3) << 4) | ((b5 >> 4) & 15);
                    decodedPixels[6] = ((b5 & 15) << 2) | ((b6 >> 6) & 3);
                    decodedPixels[7] = b6 & 63;
                    
                    // Copy only the pixels we need for this row
                    int pixelsToTake = Math.min(8, tileWidth - pixelsProcessed);
                    for (int p = 0; p < pixelsToTake && pos < pixels.length; p++) {
                        pixels[pos++] = decodedPixels[p];
                    }
                    pixelsProcessed += pixelsToTake;
                } else {
                    // Not enough data, fill with zeros
                    while (pixelsProcessed < tileWidth && pos < pixels.length) {
                        pixels[pos++] = 0;
                        pixelsProcessed++;
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
        int b1, b2, b3, b4, b5, b6;
        stride *= bytesPerRow;
        for (int i=0; i<tileHeight; i++) {
            // Process pixels for each row
            int pixelsProcessed = 0;
            
            // Process 8-pixel blocks for encoding
            while (pixelsProcessed < tileWidth) {
                // Gather up to 8 pixels for encoding
                int[] pixelsToEncode = new int[8];
                int pixelsToTake = Math.min(8, tileWidth - pixelsProcessed);
                
                for (int p = 0; p < 8; p++) {
                    if (p < pixelsToTake && pos < pixels.length) {
                        pixelsToEncode[p] = pixels[pos++];
                    } else {
                        pixelsToEncode[p] = 0; // Pad with zeros
                    }
                }
                
                // Encode 8 pixels into 6 bytes
                b1 = (pixelsToEncode[0] & 63) << 2;
                b1 |= (pixelsToEncode[1] & 48) >> 4;
                
                b2 = (pixelsToEncode[1] & 15) << 4;
                b2 |= (pixelsToEncode[2] & 60) >> 2;
                
                b3 = (pixelsToEncode[2] & 3) << 6;
                b3 |= (pixelsToEncode[3] & 63);
                
                b4 = (pixelsToEncode[4] & 63) << 2;
                b4 |= (pixelsToEncode[5] & 48) >> 4;
                
                b5 = (pixelsToEncode[5] & 15) << 4;
                b5 |= (pixelsToEncode[6] & 60) >> 2;
                
                b6 = (pixelsToEncode[6] & 3) << 6;
                b6 |= (pixelsToEncode[7] & 63);
                
                // Write the 6 bytes
                if (ofs + 5 < bits.length) {
                    bits[ofs++] = (byte)b6; // byte 1: 0000 0011
                    bits[ofs++] = (byte)b5; // byte 2: 1111 2222
                    bits[ofs++] = (byte)b4; // byte 3: 2233 3333
                    bits[ofs++] = (byte)b3; // byte 4: 4444 4455
                    bits[ofs++] = (byte)b2; // byte 5: 5555 6666
                    bits[ofs++] = (byte)b1; // byte 6: 6677 7777
                } else {
                    // Not enough space, skip
                    break;
                }
                
                pixelsProcessed += pixelsToTake;
            }
            ofs += stride;
        }
    }

}