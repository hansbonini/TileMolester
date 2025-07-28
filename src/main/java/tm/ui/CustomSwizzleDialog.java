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

package tm.ui;

import tm.tilecodecs.TileCodec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
*
* Dialog for configuring custom swizzle settings.
*
**/

public class CustomSwizzleDialog extends JDialog {
    
    private boolean isOK = false;
    private TileCodec codec;
    
    private JSpinner blockWidthSpinner;
    private JSpinner blockHeightSpinner;
    private JCheckBox mortonOrderCheckbox;
    
    private JButton okButton;
    private JButton cancelButton;

    /**
     * Creates a new custom swizzle dialog.
     */
    public CustomSwizzleDialog(Frame parent, TileCodec codec) {
        super(parent, "Custom Swizzle Settings", true);
        this.codec = codec;
        initComponents();
        setupDialog();
    }

    /**
     * Initializes the dialog components.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Main panel with form controls
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Block Width
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Block Width:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        blockWidthSpinner = new JSpinner(new SpinnerNumberModel(
            codec != null ? codec.getCustomBlockWidth() : 4, 1, 256, 1));
        mainPanel.add(blockWidthSpinner, gbc);
        
        // Block Height
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Block Height:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        blockHeightSpinner = new JSpinner(new SpinnerNumberModel(
            codec != null ? codec.getCustomBlockHeight() : 4, 1, 256, 1));
        mainPanel.add(blockHeightSpinner, gbc);
        
        // Morton Order checkbox
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        mortonOrderCheckbox = new JCheckBox("Use Morton Order (Z-order curve)", 
            codec != null ? codec.getCustomMortonOrder() : true);
        mainPanel.add(mortonOrderCheckbox, gbc);
        
        // Description label
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel descLabel = new JLabel("<html><i>Morton order interleaves X,Y bits for cache-friendly access patterns.<br>" +
                                     "Linear order stores pixels sequentially within each block.</i></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 11.0f));
        mainPanel.add(descLabel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            saveSettings();
            isOK = true;
            setVisible(false);
        });
        
        cancelButton.addActionListener(e -> {
            isOK = false;
            setVisible(false);
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets up the dialog properties.
     */
    private void setupDialog() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
        
        // Set default button
        getRootPane().setDefaultButton(okButton);
        
        // ESC key closes dialog
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isOK = false;
                setVisible(false);
            }
        });
    }

    /**
     * Saves the settings to the codec.
     */
    private void saveSettings() {
        if (codec != null) {
            codec.setCustomBlockWidth((Integer) blockWidthSpinner.getValue());
            codec.setCustomBlockHeight((Integer) blockHeightSpinner.getValue());
            codec.setCustomMortonOrder(mortonOrderCheckbox.isSelected());
        }
    }

    /**
     * Returns whether the OK button was pressed.
     */
    public boolean isOK() {
        return isOK;
    }
}
