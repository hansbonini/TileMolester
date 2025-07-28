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

package tm.canvases;

import tm.TMPalette;
import tm.ui.TMUI;
import tm.ui.TMView;
import tm.tilecodecs.TileCodec;
import tm.treenodes.*;
import tm.reversibleaction.*;
import tm.utils.TMTools;

import java.nio.IntBuffer;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
*
* Tile canvas where pixels can be edited.
*
**/

public class TMEditorCanvas extends TMTileCanvas implements MouseInputListener {

	
    // custom cursors
    private Cursor zoomCursor;
    private Cursor pickupCursor;
    private Cursor brushCursor;
    private Cursor fillCursor;

    private TMSelectionCanvas selectionCanvas;

    private boolean isSelecting=false;
    private boolean isDrawingLine=false;
    private int selX1, selY1, selX2, selY2;

    private int lineX1, lineY1, lineX2, lineY2;
    private int colorDraw;
    private int filledColor;
    private int currentCol=0, currentRow=0;

    private TMUI ui;
    private TMView view;

    private Point moveViewPoint;
    private Point moveMousePoint;

    private Vector modifiedTiles = new Vector(); // tiles modified by operation
    private Vector modifiedPixels = new Vector();
    private Point[][] gridCoords;

    private int blockWidth=1;
    private int blockHeight=1;
    private boolean rowInterleaved=false;
    private String swizzlePattern = tm.tilecodecs.TileCodec.SWIZZLE_NONE;
	private boolean showBlockGrid=false;

/**
*
* Creates an editor canvas for the given TMUI instance.
*
**/

    public TMEditorCanvas(TMUI ui, TMView view) {
        super(view.getFileImage().getContents());
        this.ui = ui;
        this.view = view;
        selectionCanvas = null;
        addMouseListener(this);
        addMouseMotionListener(this);

        // create custom cursors
        ClassLoader cl = getClass().getClassLoader();
        zoomCursor = Toolkit.getDefaultToolkit().createCustomCursor(new ImageIcon(cl.getResource("icons/ZoomCursor24.gif")).getImage(), new Point(8,7), "Zoom");
        pickupCursor = Toolkit.getDefaultToolkit().createCustomCursor(new ImageIcon(cl.getResource("icons/DropperCursor24.gif")).getImage(), new Point(6,19), "Dropper");
        brushCursor = Toolkit.getDefaultToolkit().createCustomCursor(new ImageIcon(cl.getResource("icons/BrushCursor24.gif")).getImage(), new Point(5,19), "Brush");
		fillCursor = Toolkit.getDefaultToolkit().createCustomCursor(new ImageIcon(cl.getResource("icons/FillCursor24.gif")).getImage(), new Point(5,16), "Fill");
		

    }

/**
*
* Paints the editor canvas.
*
**/

    public void paintComponent(Graphics g) {
		super.paintComponent(g);
        drawBlockGrid(g);
        drawIntermediateSelection(g);
	}
	

/**
*
* Draws the block grid.
*
**/

    private void drawBlockGrid(Graphics g) {
        if (isBlockGridVisible()) {
            g.setColor(Color.blue);    // gridline color
            int blockRows = rows / blockHeight;
            if ((rows % blockHeight) != 0) blockRows++;
            int blockCols = cols / blockWidth;
            if ((cols % blockWidth) != 0) blockCols++;
            // draw horizontal lines
            for (int i=1; i<blockRows; i++) {
                g.fillRect(0, (int)(i*scale*blockHeight*8), getWidth(), 2);
            }
            // draw vertical lines
            for (int i=1; i<blockCols; i++) {
                g.fillRect((int)(i*scale*blockWidth*8), 0, 2, getHeight());
            }
        }
    }

/**
*
* Draws a frame around the current selection, if there is one.
*
**/

    private void drawIntermediateSelection(Graphics g) {
        if (isSelecting) {
            int x1 = selX1;
            int x2 = selX2;
            int y1 = selY1;
            int y2 = selY2;
            if (x1 > x2) {
                x1 = selX2;
                x2 = selX1;
            }
            if (y1 > y2) {
                y1 = selY2;
                y2 = selY1;
            }
            int dim = getScaledTileDim();
            g.setColor(Color.white);
            g.drawRect(x1*dim, y1*dim, (x2-x1+1)*dim-1, (y2-y1+1)*dim-1);
        }
    }

///////////////////////////////////////////////////////////////////////////////
// Drawing/tool-related

/**
*
* The mouse was clicked inside the canvas.
*
**/

    public void mouseClicked(MouseEvent e) {
        e.getClickCount();
    }

/**
*
* Mouse entered the editor canvas.
* Switch to the proper cursor according to the current tool.
*
**/

    public void mouseEntered(MouseEvent e) {
        setToolTipText("");
        TMTools.ToolType tool = ui.getToolType();
        if (tool.equals(TMTools.ToolType.SELECT_TOOL)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
        else if (tool.equals(TMTools.ToolType.ZOOM_TOOL)) {
            setCursor(zoomCursor);
        }
        else if (tool.equals(TMTools.ToolType.PICKUP_TOOL)) {
            setCursor(pickupCursor);
        }
        else if (tool.equals(TMTools.ToolType.BRUSH_TOOL)) {
            setCursor(brushCursor);
        }
        else if (tool.equals(TMTools.ToolType.LINE_TOOL)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
        else if (tool.equals(TMTools.ToolType.FILL_TOOL)) {
            setCursor(fillCursor);
        }
        else if (tool.equals(TMTools.ToolType.REPLACE_TOOL)) {
            setCursor(brushCursor);
        }
        else if (tool.equals(TMTools.ToolType.MOVE_TOOL)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

/**
*
* The mouse exited the canvas.
*
**/

    public void mouseExited(MouseEvent e) {
        ui.hideStatusBarCoords();
    }

/**
*
* Initiates relevant tool action.
*
**/

    public void mousePressed(MouseEvent e) {
        // disable the keyboard events while a mouse action is in progress
        view.setKeysEnabled(false);
        //
//        if (tool != TMUI.SELECT_TOOL) {
            maybeApplySelection();
//        }
        // get the current tool
        TMTools.ToolType tool = ui.getToolType();
        // get pixel coordinate
        int x = (int)(e.getX() / scale);
        int y = (int)(e.getY() / scale);

        switch (tool) {
            case SELECT_TOOL:
                // figure out starting (x,y) tile coords
                selX1 = x / 8;
                selY1 = y / 8;
                //
                isSelecting = false;
                repaint();
                break;
            case ZOOM_TOOL:
                if (e.getButton() == MouseEvent.BUTTON1) {
                    ui.doZoomInCommand();
                }
                else {
                    ui.doZoomOutCommand();
                }
                break;
            case PICKUP_TOOL:
                // get pixel under cursor
                int colorPickup = getPixel(x, y);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // set as foreground color
                    ui.setFGColor(colorPickup);
                }
                else {
                    // set as background color
                    ui.setBGColor(colorPickup);
                }
                break;
            case BRUSH_TOOL:
                colorDraw = getDrawColor(e.getButton());
                drawLine(x, y, x, y, true);
                redraw();
                lineX1 = x;
                lineY1 = y;
                break;
            case LINE_TOOL:
                colorDraw = getDrawColor(e.getButton());
                drawLine(x, y, x, y, false);
                redraw();
                lineX1 = x;
                lineY1 = y;
                lineX2 = x;
                lineY2 = y;
                isDrawingLine = true;
                break;
            case FILL_TOOL:
                // get pixel under cursor
                int colorFill = getPixel(x, y);
                // get draw color
                colorDraw = getDrawColor(e.getButton());
                if (colorDraw != colorFill) {
                    // fill!
                    filledColor = colorFill;
                    // fillRecursive(x, y);
                    floodFill(x, y);
                    commitDrawingOperation("Flood Fill");   // i18n
                }
                break;
            case REPLACE_TOOL:
                // get pixel under cursor
                int colorRplace = getPixel(x, y);
                colorDraw = getDrawColor(e.getButton());
                // replace all occurences of color
                for (int i=0; i<canvasHeight; i++) {
                    for (int j=0; j<canvasWidth; j++) {
                        if (getPixel(j, i) == colorRplace) {
                            setPixelTraceable(j, i, colorDraw);
                        }
                    }
                }
                commitDrawingOperation("Color Replace");    // i18n
                break;
            case MOVE_TOOL:
                moveMousePoint = new Point(e.getPoint());
                break;
        }
    }

/**
*
* Called when user has pressed mouse button and is moving mouse.
*
**/

    public void mouseDragged(MouseEvent e) {
        // get the current tool
        TMTools.ToolType tool = ui.getToolType();
        // get pixel coordinate
        int x = (int)(e.getX() / scale);
        int y = (int)(e.getY() / scale);
        // bounds check
        if (x < 0) {
            x = 0;
        }
        else if (x >= canvasWidth) {
            x = canvasWidth - 1;
        }
        if (y < 0) {
            y = 0;
        }
        else if (y >= canvasHeight) {
            y = canvasHeight - 1;
        }

        switch (tool) {
            case SELECT_TOOL:
                isSelecting = true;
                // update selection
                selX2 = x / 8;
                selY2 = y / 8;
                //
                repaint();
                break;
            case BRUSH_TOOL:
                drawLine(lineX1, lineY1, x, y, true);
                redraw();
                lineX1 = x;
                lineY1 = y;
                break;
            case LINE_TOOL:
                if (isDrawingLine) {
                    if ((x != lineX2) || (y != lineY2)) {   // only if coordinates changed...
                        // "erase" old line
                        drawLine(lineX1, lineY1, lineX2, lineY2, false);
                        // draw new line
                        drawLine(lineX1, lineY1, x, y, false);
                        lineX2 = x;
                        lineY2 = y;
                        redraw();
                    }
                }
            case MOVE_TOOL:
                Point p = e.getPoint();
                int dx = p.x - moveMousePoint.x;
                int dy = p.y - moveMousePoint.y;

                TMView view = (TMView)ui.getDesktop().getSelectedFrame();
                JScrollBar hsb = view.getScrollPane().getHorizontalScrollBar();
                JScrollBar vsb = view.getScrollPane().getVerticalScrollBar();
                int newXpos = hsb.getValue() + dx;
                int newYpos = vsb.getValue() + dy;

                if (newXpos < hsb.getMinimum()) newXpos = hsb.getMinimum();
                else if (newXpos > hsb.getMaximum()) newXpos = hsb.getMaximum();
                if (newYpos < vsb.getMinimum()) newYpos = vsb.getMinimum();
                else if (newYpos > vsb.getMaximum()) newYpos = vsb.getMaximum();

                hsb.setValue(newXpos);
                hsb.revalidate();
                vsb.setValue(newYpos);
                vsb.revalidate();
                view.getScrollPane().revalidate();
                view.revalidate();

                moveMousePoint = new Point(p);
                break;
            default:
                break;
        }

        // update status bar coords
        currentCol = e.getX() / getScaledTileDim();
        currentRow = e.getY() / getScaledTileDim();
        ui.refreshStatusBar();
    }

/**
*
* Terminates relevant tool action.
*
**/

    public void mouseReleased(MouseEvent e) {
        // get the current tool
        TMTools.ToolType tool = ui.getToolType();

        switch (tool) {
            case SELECT_TOOL:
                if (isSelecting) {
                    // end selection
                    isSelecting = false;
                    if (selX1 > selX2) {
                        // swap
                        int temp = selX1;
                        selX1 = selX2;
                        selX2 = temp;
                    }
                    if (selY1 > selY2) {
                        // swap
                        int temp = selY1;
                        selY1 = selY2;
                        selY2 = temp;
                    }

                    makeSelection(selX1, selY1, selX2-selX1+1, selY2-selY1+1);

                    view.addReversibleAction(
                        new ReversibleNewSelectionAction(selectionCanvas,this)
                    );

                }
                else {
                    // encode selection, if there is one
                    // TODO: what is this doing here?
                }
                if (hasSelection()) {
                    selectionCanvas.maybeShowPopup(e);
                }
                break;
            case BRUSH_TOOL:
                commitDrawingOperation("Brush");    // i18n
                break;
            case LINE_TOOL:
                drawLine(lineX1, lineY1, lineX2, lineY2, false);
                // draw the final line
                isDrawingLine = false;
                drawLine(lineX1, lineY1, lineX2, lineY2, true);
                commitDrawingOperation("Line"); // i18n
                break;
            default:
                break;
        }
        // enabled keyboard events again
        view.setKeysEnabled(true);
    }

/**
*
* The mouse was moved within the canvas.
*
**/

    public void mouseMoved(MouseEvent e) {
        // get pixel coordinate
        int x = (int)(e.getX() / scale);
        int y = (int)(e.getY() / scale);

        if (ui.getToolType().equals(TMTools.ToolType.PICKUP_TOOL)) {
            // get pixel under cursor
            int color = getPixel(x, y);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            setToolTipText("R:"+r+" G:"+g+" B:"+b);
        }

        // update status bar coords
        currentCol = e.getX() / getScaledTileDim();
        currentRow = e.getY() / getScaledTileDim();
        ui.refreshStatusBar();
    }

/**
*
* Non-recursive algorithm that performs flood fill, starting at location (x,y).
*
**/

    public void floodFill(int x, int y) {
        Vector seeds = new Vector();
        seeds.add(new Point(x, y));
        while (!seeds.isEmpty()) {
            Point p = (Point)seeds.remove(0);
            x = p.x;
            y = p.y;
            if (getPixel(x, y) == colorDraw) continue;
            setPixelTraceable(x, y, colorDraw);

            // find left side, filling along the way
            int left = x-1;
            while ((left >= 0) && (getPixel(left, y) == filledColor)) {
                setPixelTraceable(left--, y, colorDraw);
            }
            left++;

            // find right side, filling along the way
            int right = x+1;
            while ((right < canvasWidth) && (getPixel(right, y) == filledColor)) {
                setPixelTraceable(right++, y, colorDraw);
            }
            right--;

            // check row above
            y--;
            if (y >= 0) {
                for (int i=left; i<=right; i++) {
                    if (getPixel(i, y) == filledColor) {
                        seeds.add(new Point(i, y));
                    }
                }
            }

            // check row below
            y += 2;
            if (y < canvasHeight) {
                for (int i=left; i<=right; i++) {
                    if (getPixel(i, y) == filledColor) {
                        seeds.add(new Point(i, y));
                    }
                }
            }
        }
    }

/**
*
* Replaces color with drawing color.
* This isn't used anymore because it's recursive at the pixel granularity,
* which caused stack overflows when filling moderately large areas.
* See the above method for a non-recursive implementation.
*
**/

    private void fillRecursive(int x, int y) {
        // bounds check
        if ((x < 0) || (y < 0) || (x >= canvasWidth) || (y >= canvasHeight)) return;

        // color check
        if (getPixel(x, y) != filledColor) return;

        // replace color
        setPixelTraceable(x, y, colorDraw);

        // call recursively
        fillRecursive(x+1, y);
        fillRecursive(x-1, y);
        fillRecursive(x, y+1);
        fillRecursive(x, y - 1);
    }

/**
*
* Draws a line from coordinates (x1,y1) to (x2,y2) in the pixel buffer,
* using the current draw color.
*
**/

    private void drawLine(int x1, int y1, int x2, int y2, boolean trace) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        double delta;
        if (dx != 0) {
            delta = (double)dy / (double)dx;
        }
        else {
            delta = 100000.0; // infinity
        }
        if (delta > 1.0 || delta < -1.0) {
            delta = (double)dx / (double)dy;
            // step in y direction
            if (dy < 0) {
                double i = x2;
                for (int j=y2; j<=y1; j++) {
                    if (trace)
                        setPixelTraceable((int)i, j, colorDraw);
                    else
                        xorPixel((int)i, j);
                    i += delta;
                }
            }
            else {
                double i = x1;
                for (int j=y1; j<=y2; j++) {
                    if (trace)
                        setPixelTraceable((int)i, j, colorDraw);
                    else
                        xorPixel((int)i, j);
                    i += delta;
                }
            }
        }
        else {
            // step in x direction
            if (dx < 0) {
                double j = y2;
                for (int i=x2; i<=x1; i++) {
                    if (trace)
                        setPixelTraceable(i, (int)j, colorDraw);
                    else
                        xorPixel(i, (int)j);
                    j += delta;
                }
            }
            else {
                double j = y1;
                for (int i=x1; i<=x2; i++) {
                    if (trace)
                        setPixelTraceable(i, (int)j, colorDraw);
                    else
                        xorPixel(i, (int)j);
                    j += delta;
                }
            }
        }
    }

/**
*
* Sets the pixel at coordinate (x,y) in the canvas to the specified value,
* and signals that it has been modified.
*
**/

    protected void setPixelTraceable(int x, int y, int argb) {
        // mark the tile as modified
        int tileWidth = (codec != null) ? codec.getTileWidth() : 8;
        int tileHeight = (codec != null) ? codec.getTileHeight() : 8;
        tileModified(x/tileWidth, y/tileHeight);

        setPixel(x, y, argb);
    }

/**
*
* Marks the tile at location (col,row) in the grid as modified.
*
**/

    private void tileModified(int col, int row) {
        Point p = gridCoords[row][col];
        if (!modifiedTiles.contains(p)) {
            modifiedTiles.add(p);
            // save original pixels
            int tileWidth = (codec != null) ? codec.getTileWidth() : 8;
            int tileHeight = (codec != null) ? codec.getTileHeight() : 8;
            int tileSize = tileWidth * tileHeight;
            int[] tempPixels = new int[tileSize];
            copyTilePixelsToBuffer(col, row, tempPixels, 0);
            IntBuffer ib = IntBuffer.allocate(tileSize);
            ib.put(tempPixels);
            modifiedPixels.add(ib);
        }
    }

/**
*
* Copies the pixels for the tile at location (x,y) in the grid to the
* specified buffer, starting at the specified offset.
*
**/

    public void copyTilePixelsToBuffer(int x, int y, int[] buf, int ofs) {
        int tileWidth = (codec != null) ? codec.getTileWidth() : 8;
        int tileHeight = (codec != null) ? codec.getTileHeight() : 8;
        int pixOfs = (y * tileHeight * canvasWidth) + (x * tileWidth);
        for (int i=0; i<tileHeight; i++) {
            for (int j=0; j<tileWidth; j++) {
                if (ofs < buf.length && pixOfs < pixels.length) {
                    buf[ofs++] = pixels[pixOfs++];
                } else {
                    ofs++;
                    pixOfs++;
                }
            }
            pixOfs += canvasWidth - tileWidth;
        }
    }

/**
*
* Copies the pixels in the buffer to the tile at location (x,y).
*
**/

    public void copyBufferToTilePixels(int x, int y, int[] buf, int ofs) {
        int tileWidth = (codec != null) ? codec.getTileWidth() : 8;
        int tileHeight = (codec != null) ? codec.getTileHeight() : 8;
        int pixOfs = (y * tileHeight * canvasWidth) + (x * tileWidth);
        for (int i=0; i<tileHeight; i++) {
            for (int j=0; j<tileWidth; j++) {
                if (ofs < buf.length && pixOfs < pixels.length) {
                    pixels[pixOfs++] = buf[ofs++];
                } else {
                    ofs++;
                    pixOfs++;
                }
            }
            pixOfs += canvasWidth - tileWidth;
        }
    }

/**
*
* Commits a drawing operation: Encodes all the affected tiles,
* sets modified, eventually adds to Undo buffer, redraws tiles
*
**/

    private ReversibleTileModifyAction commitDrawingOperation(String name) {
        return commitDrawingOperation(name,true);
    }

    private ReversibleTileModifyAction commitDrawingOperation(String name, boolean undoable) {
        // Undo-stuff
        BookmarkItemNode bookmark = view.createBookmark("");

        Point[] pts = new Point[modifiedTiles.size()];
        int tileWidth = (codec != null) ? codec.getTileWidth() : 8;
        int tileHeight = (codec != null) ? codec.getTileHeight() : 8;
        int tileSize = tileWidth * tileHeight;
        int[] oldPix = new int[pts.length * tileSize];
        int[] newPix = new int[pts.length * tileSize];

        for (int i=0; i<modifiedTiles.size(); i++) {
            Point p = (Point)modifiedTiles.elementAt(i);
            pts[i] = new Point(p);

            IntBuffer ib = (IntBuffer)modifiedPixels.elementAt(i);
            System.arraycopy(ib.array(), 0, oldPix, i * tileSize, tileSize);

            copyTilePixelsToBuffer(p.x, p.y, newPix, i * tileSize);

            packTile(p.x, p.y);
        }

            ReversibleTileModifyAction rtma = new ReversibleTileModifyAction(
                    name, this, bookmark, pts, oldPix, newPix
            );
        if (undoable) { //TODO: maybe will cause mem leak
            view.addReversibleAction(rtma);
        }

        redraw();

        modifiedTiles.clear();
        modifiedPixels.clear();
        return rtma; //Dirty fix; should return something simpler
    }

/**
*
* Gets the current tile column.
*
**/

    public int getCurrentCol() {
        return currentCol;
    }

/**
*
* Gets the current tile row.
*
**/

    public int getCurrentRow() {
        return currentRow;
    }

///////////////////////////////////////////////////////////////////////////////
// Selection-related

/**
*
* Copies the current selection to the specified canvas.
*
**/

    public TMSelectionCanvas copySelection() {
        if (!hasSelection()) {
            return new TMSelectionCanvas(ui, this, 0, 0, cols, rows);
        }
        else {
            return new TMSelectionCanvas(ui, selectionCanvas, 0, 0, selectionCanvas.getCols(), selectionCanvas.getRows());
        }
    }

/**
*
* Cuts the current selection.
*
**/

    public TMSelectionCanvas cutSelection() {
        if (!hasSelection()) {
            selectAll();
        }

        view.addReversibleAction(
            new ReversibleCutAction(
                selectionCanvas,
                this
            )
        );

        remove(selectionCanvas);
        repaint();
        return new TMSelectionCanvas(ui, selectionCanvas, 0, 0, selectionCanvas.getCols(), selectionCanvas.getRows());
    }

/**
*
* Pastes the contents of the given canvas onto the selection canvas.
*
**/

    public void paste(TMSelectionCanvas canvas) {
        maybeApplySelection();

        selectionCanvas = new TMSelectionCanvas(ui, canvas, 0, 0, canvas.getCols(), canvas.getRows());
        view.addReversibleAction(new ReversiblePasteSelectionAction(selectionCanvas, this));
    }

/**
*
* Sets the bgColor to specified pixels
*
**/
    public void setBlankPixels(int bgColor,int x1,int y1,int x2,int y2)
    {
        int tileWidth = (codec != null) ? codec.getTileWidth() : 8;
        int tileHeight = (codec != null) ? codec.getTileHeight() : 8;
        //erases original tiles
        for (int i=y1*tileHeight; i<y2*tileHeight; i++) {
            for (int j=x1*tileWidth; j<x2*tileWidth; j++) {
                setPixel(j, i, bgColor);
            }
        }
        // re-encode the blank tiles
        for (int i=y1; i<y2; i++) {
            for (int j=x1; j<x2; j++) {
                packTile(j, i);
            }
        }
        redraw();
    }
/**
 *
 * Recreates the spcified selection
 *
 **/
    public void makeSelection(TMSelectionCanvas selection)
    {
        selectionCanvas = selection;
        int dim = selectionCanvas.getScaledTileDim();
        int x1 = selectionCanvas.getX() / dim;
        int y1 = selectionCanvas.getY()/ dim;
        int w = selectionCanvas.getWidth()/ dim;
        int h = selectionCanvas.getHeight()/ dim;
        showSelection(selectionCanvas, x1,y1);

        setBlankPixels(ui.getBGColor(),x1,y1,x1+w,y1+h);
    }
/**
*
* Copies the tiles in the rectangle starting at (x1,y1) of width
* w and height h.
*
**/

    public void makeSelection(int x1, int y1, int w, int h) {
        selectionCanvas = new TMSelectionCanvas(ui, this, x1, y1, w, h);
        showSelection(selectionCanvas, x1, y1);

        setBlankPixels(ui.getBGColor(), x1, y1, x1 + w,y1+h);
    }

/**
*
* Show selection at the specified tile grid coordinate.
*
**/

    public void showSelection(TMSelectionCanvas selectionCanvas, int x, int y) {
        this.selectionCanvas = selectionCanvas;
        add(selectionCanvas);
        selectionCanvas.setScale(scale);
        int dim = getScaledTileDim();
        selectionCanvas.setLocation(x*dim, y*dim);
        selectionCanvas.setVisible(true);
        redraw();
    }

/**
*
* Encodes the contents of the selection canvas at its current position.
*
**/
public ReversibleTileModifyAction encodeSelection()
{
   return encodeSelection(true);
}

    public ReversibleTileModifyAction encodeSelection(boolean userCanUndo) {
        int srcx = 0;
        int srcy = 0;
        int destx = selectionCanvas.getX() / getScaledTileDim();
        int desty = selectionCanvas.getY() / getScaledTileDim();

        // bounds check
        if (destx < 0) {
            srcx = -destx;
            destx = 0;
        }
        if (desty < 0) {
            srcy = -desty;
            desty = 0;
        }

        // calculate width and height of tile grid to encode
        int w = selectionCanvas.getCols() - srcx;
        int h = selectionCanvas.getRows() - srcy;
        if (destx + w > cols) {
            w = cols - destx;
        }
        if (desty + h > rows) {
            h = rows - desty;
        }

        // Undo-stuff
        for (int i=0; i<h; i++) {
            for (int j=0; j<w; j++) {
                tileModified(destx+j, desty+i);
            }
        }

        //
        int srcColor;
        int srcBPP = selectionCanvas.getCodec().getBitsPerPixel();
        int destBPP = codec.getBitsPerPixel();

        // the following variables are only valid when src and/or dest are palettized
        TMPalette srcPalette = selectionCanvas.getPalette();
        int colorCount = codec.getColorCount();
        int colorIndex = palIndex * colorCount;
        int srcPalIndex = selectionCanvas.getPalIndex();
        int srcColorCount = selectionCanvas.getCodec().getColorCount();
        int srcColorIndex = srcPalIndex * srcColorCount;

        // copy tiles from selection canvas
        for (int i=0; i<h; i++) {
            for (int j=0; j<w; j++) {
                // copy pixels for one tile. 4 cases:
                // 1. palettized to palettized. map indices from source to dest. (or closest match?)
                // 2. palettized to non-palettized. Map indices to RGB values.
                // 3. non-palettized to palettized. Find closest entry match in palette.
                // 4. non-palettized to non-palettized. Copy RGB values unchanged.
                if ((srcBPP <= 8) && (destBPP <= 8)) {
                    // 1. palettized to palettized
                    for (int p=0; p<8; p++) {
                        for (int q=0; q<8; q++) {
                            srcColor = srcPalette.indexOf(srcColorIndex, selectionCanvas.getPixel((srcx+j)*8+q, (srcy+i)*8+p));
                            setPixel((destx+j)*8+q, (desty+i)*8+p, palette.getEntryRGB(colorIndex + srcColor));
                            // To find best match: use these lines instead
                            // srcColor = selectionCanvas.getPixel((srcx+j)*8+q, (srcy+i)*8+p));
                            // setPixel((destx+j)*8+q, (desty+i)*8+p, palette.closestMatchingEntryRGB(colorIndex, colorCount, srcColor));
                        }
                    }
                }
                else if (srcBPP <= 8) {
                    // 2. palettized to non-palettized
                    for (int p=0; p<8; p++) {
                        for (int q=0; q<8; q++) {
                            srcColor = srcPalette.indexOf(srcColorIndex, selectionCanvas.getPixel((srcx+j)*8+q, (srcy+i)*8+p));
                            setPixel((destx+j)*8+q, (desty+i)*8+p, srcPalette.getEntryRGB(srcColor));
                        }
                    }
                }
                else if (destBPP <= 8) {
                    // 3. non-palettized to palettized
                    for (int p=0; p<8; p++) {
                        for (int q=0; q<8; q++) {
                            srcColor = selectionCanvas.getPixel((srcx+j)*8+q, (srcy+i)*8+p);
                            setPixel((destx+j)*8+q, (desty+i)*8+p, palette.closestMatchingEntryRGB(colorIndex, colorCount, srcColor));
                        }
                    }
                }
                else {
                    // 4. non-palettized to non-palettized
                    for (int p=0; p<8; p++) {
                        for (int q=0; q<8; q++) {
                            srcColor = selectionCanvas.getPixel((srcx+j)*8+q, (srcy+i)*8+p);
                            setPixel((destx+j)*8+q, (desty+i)*8+p, srcColor);
                        }
                    }
                }
            }
        }

        ReversibleTileModifyAction modifiedTiles = commitDrawingOperation("Encode Selection", userCanUndo);

        // remove the selection
        remove(selectionCanvas);

        return modifiedTiles;
    }

/**
*
* Applies the selection canvas at its current position
* if it is actually valid (i.e. visible).
*
**/

    public void maybeApplySelection() {
        if (hasSelection()) {
            view.addReversibleAction(
                    new ReversibleApplySelectionAction(selectionCanvas,this)
            );
        }
    }

/**
*
* Gets the canvas that is currently regarded as the selection canvas.
* If the selection canvas itself is visible, it is returned; otherwise
* the whole editor canvas is regarded as the selection and is returned.
*
**/

    public TMTileCanvas getSelectionCanvas() {
        if (hasSelection()) {
            return selectionCanvas;
        }
        else {
            return this;    // editorCanvas
        }
    }

/**
*
* Gets the current drawing color for the specified mouse button.
*
**/

    private int getDrawColor(int button) {
        return (button == MouseEvent.BUTTON1) ? ui.getFGColor() : ui.getBGColor();
    }

/**
*
* Clears the current selection.
*
**/

    public void clearSelection() {
        view.addReversibleAction(
            new ReversibleClearAction(selectionCanvas,this)
        );

        remove(selectionCanvas);
        repaint();
    }

/**
*
* Flips the current selection horizontally.
*
**/

    public void flipSelectionHorizontally() {
        TMTileCanvas sp = getSelectionCanvas();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Mirror",
                sp,
                ReversibleActionsSelectionAction.MIRROR_ACTION
            )
        );

        sp.mirror();
        sp.redraw();
    }

/**
*
* Flips the current selection vertically.
*
**/

    public void flipSelectionVertically() {
        TMTileCanvas sp = getSelectionCanvas();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Flip",
                sp,
                ReversibleActionsSelectionAction.FLIP_ACTION
            )
        );

        sp.flip();
        sp.redraw();
    }

/**
*
* Stretches the selection from the current # of columns and rows
* to the specified # of columns and rows.
*
**/

    public void stretchSelection(int cols, int rows) {
        TMTileCanvas sp = getSelectionCanvas();
        if (sp == selectionCanvas) {

            view.addReversibleAction(
                new ReversibleStretchAction(
                    selectionCanvas,
                    cols,
                    rows
                )
            );
            sp.stretchTo(cols, rows);
            sp.redraw();
        }
    }

/**
*
* Rotates the current selection clockwise.
*
**/

    public void rotateSelectionClockwise() {
        TMTileCanvas sp = getSelectionCanvas();
        sp.rotateRight();
        sp.redraw();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Rotate Right",
                sp,
                ReversibleActionsSelectionAction.ROTATERIGHT_ACTION
            )
        );

    }

/**
*
* Rotates the current selection counter-clockwise.
*
**/

    public void rotateSelectionCounterClockwise() {
        TMTileCanvas sp = getSelectionCanvas();
        sp.rotateLeft();
        sp.redraw();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Rotate Left",
                sp,
                ReversibleActionsSelectionAction.ROTATELEFT_ACTION
            )
        );
    }

/**
*
* Shifts the current selection one pixel left.
*
**/

    public void shiftSelectionLeft() {
        TMTileCanvas sp = getSelectionCanvas();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Shift Left",
                sp,
                ReversibleActionsSelectionAction.SHIFTLEFT_ACTION
            )
        );

        sp.shiftLeft();
        sp.redraw();
    }

/**
*
* Shifts the current selection one pixel right.
*
**/

    public void shiftSelectionRight() {
        TMTileCanvas sp = getSelectionCanvas();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Shift Right",
                sp,
                ReversibleActionsSelectionAction.SHIFTRIGHT_ACTION
            )
        );

        sp.shiftRight();
        sp.redraw();
    }

/**
*
* Shifts the current selection one pixel up.
*
**/

    public void shiftSelectionUp() {
        TMTileCanvas sp = getSelectionCanvas();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Shift Up",
                sp,
                ReversibleActionsSelectionAction.SHIFTUP_ACTION
            )
        );

        sp.shiftUp();
        sp.redraw();
    }

/**
*
* Shifts the current selection one pixel down.
*
**/

    public void shiftSelectionDown() {
        TMTileCanvas sp = getSelectionCanvas();

        view.addReversibleAction(
            new ReversibleActionsSelectionAction(
                "Shift Down",
                sp,
                ReversibleActionsSelectionAction.SHIFTDOWN_ACTION
            )
        );

        sp.shiftDown();
        sp.redraw();
    }

/**
*
* Selects all tiles.
*
**/

    public void selectAll() {
        maybeApplySelection();
        makeSelection(0, 0, cols, rows);
        view.addReversibleAction(
                new ReversibleNewSelectionAction(selectionCanvas,this)
        );
    }

/**
*
* Sets the drawing scale (zoom).
*
**/

    public void setScale(double scale) {
        super.setScale(scale);
        if (hasSelection()) {
            selectionCanvas.setScale(scale);
            selectionCanvas.snapToGrid();
        }
    }

/**
*
* Returns whether the user is selecting a grid of tiles.
*
**/

    public boolean isSelecting() {
        return isSelecting;
    }

/**
*
* Gets the upper left column # of the selection.
*
**/

    public int getSelX1() {
        return selX1;
    }

/**
*
* Gets the upper left row # of the selection.
*
**/

    public int getSelY1() {
        return selY1;
    }

/**
*
* Gets the lower right column # of the selection.
*
**/

    public int getSelX2() {
        return selX2;
    }

/**
*
* Gets the lower right row # of the selection.
*
**/

    public int getSelY2() {
        return selY2;
    }

/**
*
* Returns whether the user is currently drawing a line.
*
**/

    public boolean isDrawingLine() {
        return isDrawingLine;
    }

/**
*
* Gets x1 coordinate of line.
*
**/

    public int getLineX1() {
        return lineX1;
    }

/**
*
* Gets y1 coordinate of line.
*
**/

    public int getLineY1() {
        return lineY1;
    }

/**
*
* Gets x2 coordinate of line.
*
**/

    public int getLineX2() {
        return lineX2;
    }

/**
*
* Gets y2 coordinate of line.
*
**/

    public int getLineY2() {
        return lineY2;
    }

/**
*
* Returns whether there is a selection present. TODO FIX
*
**/

    public boolean hasSelection() {
        return (getComponentCount() == 1);
    }

/**
*
* Sets the tile grid size.
*
**/

    public void setGridSize(int cols, int rows) {
        super.setGridSize(cols, rows);

        gridCoords = new Point[rows][cols];
        for (int i=0; i<rows; i++) {
            for (int j=0; j<cols; j++) {
                gridCoords[i][j] = new Point(j, i);
            }
        }
    }

/**
*
* Gets the view.
*
**/

    public TMView getView() {
        return view;
    }

/**
*
* Kills the view ref. (Futile attempt at getting the garbage collector to
* free the JInternalFrame.)
*
**/

    public void killViewRef() {
        view = null;
    }

/**
*
* Gets the starting offset of the data for the tile at position (x,y)
* in the grid.
*
**/

    protected int getTileBitsOffset(int x, int y) {
        int blockRowSize = blockHeight * getRowSize();
        int blockLineSize = blockWidth * codec.getTileSize();
        int blockSize = blockHeight * blockLineSize;

        // point relOfs to beginning of relevant block data
        int blockX = x / blockWidth;
        int blockY = y / blockHeight;
        int relOfs = (blockY * blockRowSize) + (blockX * blockSize);

        // point to tile within block
        int tileX = x % blockWidth;
        int tileY = y % blockHeight;
        if (rowInterleaved) {
            // point to relevant set of (even,odd) row-tuple
            relOfs += (tileY >> 1) * (blockLineSize << 1);
            tileX <<= 1;
            if ((mode == TileCodec.MODE_2D) && (tileX >= blockWidth)) {
                relOfs += blockLineSize;
                tileX -= blockWidth;
            }
            if ((tileY % 2) == 0) {
                // even tile (0, 2, 4, ...)
                relOfs += tileX * getTileIncrement();
            }
            else {
                // odd tile (1, 3, 5, ...)
                tileX++;
                relOfs += tileX * getTileIncrement();
            }
        }
        else {
            relOfs += (tileY * blockLineSize) + (tileX * getTileIncrement());
        }

        int absOfs = relOfs + offset;
        // range check
        int limit = 0;
        if (mode == TileCodec.MODE_1D) {
            limit = bits.length - codec.getTileSize();
        }
        else {
            limit = bits.length - getRowIncrement();
        }
        if (absOfs <= limit) return absOfs;
        return -1;
    }

/**
*
* Gets the stride.
*
**/

    public int getStride() {
        return (mode == TileCodec.MODE_1D) ? 0 : blockWidth-1;
    }

/**
*
* Turns the block grid on or off.
*
**/

    public void setBlockGridVisible(boolean showBlockGrid) {
        this.showBlockGrid = showBlockGrid;
    }

/**
*
* Gets the visibility status of the block grid.
*
**/

    public boolean isBlockGridVisible() {
        return showBlockGrid;
    }

/**
*
* Sets the block dimensions.
*
**/

    public void setBlockDimensions(int blockWidth, int blockHeight) {
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
    }

/**
*
* Gets the block width (in # of tiles).
*
**/

    public int getBlockWidth() {
        return blockWidth;
    }

/**
*
* Gets the block height (in # of tiles).
*
**/

    public int getBlockHeight() {
        return blockHeight;
    }

/**
*
* Sets row-interleaved state.
*
**/

    public void setRowInterleaveBlocks(boolean rowInterleaved) {
        this.rowInterleaved = rowInterleaved;
    }

/**
*
* Gets row-interleaved state.
*
**/

    public boolean getRowInterleaveBlocks() {
        return rowInterleaved;
    }

/**
*
* Sets the swizzle pattern.
*
**/

    public void setSwizzlePattern(String swizzlePattern) {
        this.swizzlePattern = swizzlePattern;
        if (codec != null) {
            codec.setSwizzlePattern(swizzlePattern);
        }
    }

/**
*
* Gets the swizzle pattern.
*
**/

    public String getSwizzlePattern() {
        return swizzlePattern;
    }

}