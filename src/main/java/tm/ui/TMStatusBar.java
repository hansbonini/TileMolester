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

package tm.ui;

import tm.tilecodecs.TileCodec;
import tm.canvases.TMEditorCanvas;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

/**
*
* Tile Molester status bar.
*
**/

public class TMStatusBar extends JPanel {

    private JLabel offsetLabel = new JLabel(" ");
    private JLabel coordsLabel = new JLabel(" ");
    private JLabel codecLabel = new JLabel(" ");
    private JLabel palOffsetLabel = new JLabel(" ");
    private JLabel modeLabel = new JLabel(" ");
    private JLabel tilesLabel = new JLabel(" ");
    private JLabel messageLabel = new JLabel(" ");
    private JLabel swizzleLabel = new JLabel(" ");  // Swizzle pattern information
    
    // Tile size controls
    private JLabel tileSizeLabel = new JLabel("Tile Size:");
    private JSpinner tileWidthSpinner;
    private JSpinner tileHeightSpinner;
    
    // Block size controls
    private JLabel blockSizeLabel = new JLabel("Block Size:");
    private JSpinner blockWidthSpinner;
    private JSpinner blockHeightSpinner;
    
    private TMUI parentUI;

/**
*
* Creates the status bar.
*
**/

    public TMStatusBar(TMUI parentUI) {
        super();
        this.parentUI = parentUI;
        
        JPanel p1 = new JPanel();
        p1.setLayout(new GridLayout(1, 3));
        p1.add(messageLabel);
        p1.add(offsetLabel);
        p1.add(coordsLabel);

        JPanel p2 = new JPanel();
        p2.setLayout(new GridLayout(1, 2));
        p2.add(palOffsetLabel);
        p2.add(codecLabel);

        JPanel p3 = new JPanel();
        p3.setLayout(new GridLayout(1, 2));
        p3.add(modeLabel);
        p3.add(tilesLabel);
        
        // Only add tile size controls if parentUI is provided
        if (parentUI != null) {
            // Initialize tile size spinners
            tileWidthSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 256, 1));
            tileHeightSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 256, 1));
            
            // Initialize block size spinners
            blockWidthSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 256, 1));
            blockHeightSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 256, 1));
            
            // Set preferred size for spinners
            Dimension spinnerSize = new Dimension(50, 20);
            tileWidthSpinner.setPreferredSize(spinnerSize);
            tileHeightSpinner.setPreferredSize(spinnerSize);
            blockWidthSpinner.setPreferredSize(spinnerSize);
            blockHeightSpinner.setPreferredSize(spinnerSize);
            
            // Add change listeners to spinners
            tileWidthSpinner.addChangeListener(e -> updateTileSize());
            tileHeightSpinner.addChangeListener(e -> updateTileSize());
            blockWidthSpinner.addChangeListener(e -> updateBlockSize());
            blockHeightSpinner.addChangeListener(e -> updateBlockSize());
            
            // Create tile size panel
            JPanel p4 = new JPanel();
            p4.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
            p4.add(tileSizeLabel);
            p4.add(new JLabel("W:"));
            p4.add(tileWidthSpinner);
            p4.add(new JLabel("H:"));
            p4.add(tileHeightSpinner);
            
            // Create block size panel
            JPanel p5 = new JPanel();
            p5.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
            p5.add(blockSizeLabel);
            p5.add(new JLabel("W:"));
            p5.add(blockWidthSpinner);
            p5.add(new JLabel("H:"));
            p5.add(blockHeightSpinner);
            
            // Create swizzle panel
            JPanel p6 = new JPanel();
            p6.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
            p6.add(new JLabel("Swizzle:"));
            p6.add(swizzleLabel);

            setLayout(new GridLayout(1, 7));
            add(p1);
            add(p2);
            add(p3);
            add(p4);
            add(p5);
            add(p6);
        } else {
            setLayout(new GridLayout(1, 4));
            add(p1);
            add(p2);
            add(p3);
        }
//        pane.add(new JLabel("    "));   // just some whitespace

        //offsetLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        //coordsLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        //palOffsetLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        //codecLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        //modeLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        //tilesLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

/**
*
* Creates the status bar (legacy constructor for backward compatibility).
*
**/

    public TMStatusBar() {
        this(null);
    }

/**
*
* Updates tile size when spinner values change.
*
**/

    private void updateTileSize() {
        if (parentUI != null) {
            int width = (Integer) tileWidthSpinner.getValue();
            int height = (Integer) tileHeightSpinner.getValue();
            parentUI.setTileSize(width, height);
        }
    }

/**
*
* Updates block size when spinner values change.
*
**/

    private void updateBlockSize() {
        if (parentUI != null) {
            // Block dimensions are always independent of canvas size
            int width = (Integer) blockWidthSpinner.getValue();
            int height = (Integer) blockHeightSpinner.getValue();
            parentUI.setBlockSize(width, height);
        }
    }

/**
*
* Updates the tile size spinners to reflect current tile dimensions.
*
**/

    public void setTileSize(int width, int height) {
        if (tileWidthSpinner != null && tileHeightSpinner != null) {
            tileWidthSpinner.setValue(width);
            tileHeightSpinner.setValue(height);
        }
    }

/**
*
* Updates the block size spinners to reflect current block dimensions.
*
**/

    public void setBlockSize(int width, int height) {
        if (blockWidthSpinner != null && blockHeightSpinner != null) {
            // Temporarily remove listeners to avoid triggering updates
            ChangeListener[] widthListeners = blockWidthSpinner.getChangeListeners();
            ChangeListener[] heightListeners = blockHeightSpinner.getChangeListeners();
            
            for (ChangeListener listener : widthListeners) {
                blockWidthSpinner.removeChangeListener(listener);
            }
            for (ChangeListener listener : heightListeners) {
                blockHeightSpinner.removeChangeListener(listener);
            }
            
            // Update values
            blockWidthSpinner.setValue(width);
            blockHeightSpinner.setValue(height);
            
            // Restore listeners
            for (ChangeListener listener : widthListeners) {
                blockWidthSpinner.addChangeListener(listener);
            }
            for (ChangeListener listener : heightListeners) {
                blockHeightSpinner.addChangeListener(listener);
            }
        }
    }
    
    /**
     * Updates the block size label to show that block dimensions are always independent.
     */
    public void updateBlockSizeLabel(boolean isFullCanvas) {
        // Block dimensions are always independent of canvas size
        blockSizeLabel.setText("Block Size:");
        blockSizeLabel.setToolTipText("Block dimensions are independent of canvas size");
        // Always enable spinners since block size is always independent
        if (blockWidthSpinner != null) blockWidthSpinner.setEnabled(true);
        if (blockHeightSpinner != null) blockHeightSpinner.setEnabled(true);
    }

/**
*
* Sets the text for general message.
*
**/

    public void setMessage(String s) {
        messageLabel.setText(" "+s+" ");
    }

/**
*
* Sets the hex text that indicates the file position.
*
**/

    public void setOffset(int offset) {
        String hexOffset = Integer.toHexString(offset).toUpperCase();
        while (hexOffset.length() < 8) {
            hexOffset = "0" + hexOffset;
        }
        offsetLabel.setText(" "+hexOffset+" "); // i18n
    }

/**
*
* Sets the coordinates.
*
**/

    public void setCoords(int x, int y) {
        coordsLabel.setText(" ("+x+","+y+") ");
    }

/**
*
*
*
**/

    public void setSelectionCoords(int x1, int y1, int x2, int y2) {
        int w = Math.abs(x1 - x2) + 1;
        int h = Math.abs(y1 - y2) + 1;
        coordsLabel.setText(" ("+x1+","+y1+") -> ("+x2+","+y2+") = ("+w+","+h+")");
    }

/**
*
* Sets the text that indicates the graphics codec in use.
*
**/

    public void setCodec(String s) {
        codecLabel.setText(" "+s+" ");   // i18n
    }



/**
*
* Sets the text that indicates the graphics codec in use.
*
**/

    public void setPalOffset(int offset) {
        String hexOffset = Integer.toHexString(offset).toUpperCase();
        while (hexOffset.length() < 8) {
            hexOffset = "0" + hexOffset;
        }
        palOffsetLabel.setText(" Palette: "+hexOffset+" "); // i18n
    }


/**
*
* Sets the text that indicates the current mode.
*
**/

    public void setMode(int mode) {
        if (mode == TileCodec.MODE_1D)
            modeLabel.setText(" 1-Dimensional "); // i18n
        else
            modeLabel.setText(" 2-Dimensional "); // i18n
    }

/**
*
* Sets the text that indicates how many tiles are shown.
*
**/

    public void setTiles(int w, int h) {
        tilesLabel.setText(" "+w+"x"+h+" tiles ");  // i18n
    }

/**
*
* Called when a view has been selected.
*
**/

    public void viewSelected(TMView view) {
        TMEditorCanvas ec = view.getEditorCanvas();
        setMessage("");
        setOffset(view.getOffset());
        if (ec.isSelecting()) {
            setSelectionCoords(ec.getSelX1(), ec.getSelY1(), ec.getCurrentCol(), ec.getCurrentRow());
        }
        else if (ec.isDrawingLine()) {
            setSelectionCoords(ec.getLineX1(), ec.getLineY1(), ec.getLineX2(), ec.getLineY2());
        }
        else {
            setCoords(ec.getCurrentCol(), ec.getCurrentRow());
        }
        if (view.getTileCodec() != null) {
            setCodec(view.getTileCodec().getDescription());
            // Update tile size spinners
            setTileSize(view.getTileCodec().getTileWidth(), view.getTileCodec().getTileHeight());
        }
        else {
            setCodec("");
        }
        setPalOffset(view.getPalette().getOffset());
        setMode(view.getMode());
        setTiles(view.getCols(), view.getRows());
        
        // Update block size spinners and label
        setBlockSize(view.getBlockWidth(), view.getBlockHeight());
        updateBlockSizeLabel(view.getSizeBlockToCanvas());
        
        // Update swizzle information
        setSwizzle(view.getSwizzlePattern(), view.getTileCodec());
    }

/**
 *
 * Convenience method for setting various fields of gridbagconstraints.
 *
 */

    protected static void buildConstraints(GridBagConstraints gbc, int gx, int gy, int gw, int gh, int wx, int wy) {
        gbc.gridx = gx;
        gbc.gridy = gy;
        gbc.gridwidth = gw;
        gbc.gridheight = gh;
        gbc.weightx = wx;
        gbc.weighty = wy;
    }


	/**
	 *
	 * Sets coordinates text.
	 *
	 */
	public void setCoords(String string) {
		coordsLabel.setText(string);
	}

	/**
	 * Sets the swizzle information text.
	 */
	public void setSwizzle(String swizzlePattern, tm.tilecodecs.TileCodec codec) {
		if (swizzlePattern == null || tm.tilecodecs.TileCodec.SWIZZLE_NONE.equals(swizzlePattern)) {
			swizzleLabel.setText("None");
		} else if (tm.tilecodecs.TileCodec.SWIZZLE_CUSTOM.equals(swizzlePattern) && codec != null) {
			String mortonText = codec.getCustomMortonOrder() ? "Morton" : "Linear";
			swizzleLabel.setText(String.format("Custom... %dx%d %s", 
				codec.getCustomBlockWidth(), codec.getCustomBlockHeight(), mortonText));
		} else {
			// For predefined patterns, just show the pattern name
			swizzleLabel.setText(getSwizzleDisplayName(swizzlePattern));
		}
	}

	/**
	 * Gets a user-friendly display name for a swizzle pattern.
	 * Names match those used in the menu system for consistency.
	 */
	private String getSwizzleDisplayName(String pattern) {
		switch (pattern) {
			case tm.tilecodecs.TileCodec.SWIZZLE_BC: return "BC Texture";
			case tm.tilecodecs.TileCodec.SWIZZLE_PSP: return "PlayStation Portable";
			case tm.tilecodecs.TileCodec.SWIZZLE_NDS: return "Nintendo DS";
			case tm.tilecodecs.TileCodec.SWIZZLE_3DS: return "Nintendo 3DS";
			case tm.tilecodecs.TileCodec.SWIZZLE_WII: return "Nintendo Wii";
			case tm.tilecodecs.TileCodec.SWIZZLE_SWITCH: return "Nintendo Switch";
			default: return pattern != null ? pattern : "None";
		}
	}
}