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

package tm.modaldialog;

import tm.utils.DecimalNumberVerifier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;

/**
*
* The dialog where user can enter new tile dimensions in pixels.
*
**/

public class TMTileSizeDialog extends TMModalDialog {

    private JLabel widthLabel;
    private JLabel heightLabel;
    private JTextField widthField;
    private JTextField heightField;

/**
*
* Creates the tile size dialog.
*
**/

    public TMTileSizeDialog(Frame owner, tm.utils.Xlator xl) {
        super(owner, "Tile_Size_Dialog_Title", xl);
    }

/**
*
* Gets the tile width in pixels.
*
**/

    public int getTileWidth() {
        return Integer.parseInt(widthField.getText());
    }

/**
*
* Gets the tile height in pixels.
*
**/

    public int getTileHeight() {
        return Integer.parseInt(heightField.getText());
    }

/**
*
* Creates the dialog pane with width and height input fields.
*
**/

    protected JPanel getDialogPane() {
        widthLabel = new JLabel(xlate("Tile_Width_Prompt"));
        heightLabel = new JLabel(xlate("Tile_Height_Prompt"));
        widthField = new JTextField();
        heightField = new JTextField();

        widthLabel.setBorder(new EmptyBorder(0, 4, 0, 4));
        heightLabel.setBorder(new EmptyBorder(0, 4, 0, 4));

        JPanel widthPane = new JPanel();
        widthPane.setLayout(new BoxLayout(widthPane, BoxLayout.X_AXIS));
        widthPane.add(widthLabel);
        widthPane.add(widthField);

        JPanel heightPane = new JPanel();
        heightPane.setLayout(new BoxLayout(heightPane, BoxLayout.X_AXIS));
        heightPane.add(heightLabel);
        heightPane.add(heightField);

        JPanel p = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        p.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        buildConstraints(gbc, 0, 0, 1, 1, 100, 50);
        gbl.setConstraints(widthPane, gbc);
        p.add(widthPane);

        buildConstraints(gbc, 0, 1, 1, 1, 100, 50);
        gbl.setConstraints(heightPane, gbc);
        p.add(heightPane);

        p.setPreferredSize(new Dimension(250, 80));

        widthField.setColumns(4);
        heightField.setColumns(4);

        widthField.addKeyListener(new DecimalNumberVerifier());
        widthField.getDocument().addDocumentListener(new TMDocumentListener());
        heightField.addKeyListener(new DecimalNumberVerifier());
        heightField.getDocument().addDocumentListener(new TMDocumentListener());

        return p;
    }

    public int showDialog(int initialWidth, int initialHeight) {
        widthField.setText(Integer.toString(initialWidth));
        heightField.setText(Integer.toString(initialHeight));
        maybeEnableOKButton();
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                widthField.requestFocus();
            }
        });
        return super.showDialog();
    }

    public boolean inputOK() {
        return (!widthField.getText().equals("") && !heightField.getText().equals("")
            && (getTileWidth() > 0) && (getTileHeight() > 0)
            && (getTileWidth() <= 256) && (getTileHeight() <= 256));
    }

}
