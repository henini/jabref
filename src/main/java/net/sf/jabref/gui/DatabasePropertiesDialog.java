/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.config.SaveOrderConfig;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 31, 2005
 * Time: 10:46:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabasePropertiesDialog extends JDialog {

    private MetaData metaData;
    private BasePanel panel;
    private final JComboBox<Charset> encoding;
    private final JButton ok;
    private final JButton cancel;
    private final JTextField fileDir = new JTextField(40);
    private final JTextField fileDirIndv = new JTextField(40);
    private String oldFileVal = "";
    private String oldFileIndvVal = "";
    private SaveOrderConfig oldSaveOrderConfig;

    /* The code for "Save sort order" is copied from FileSortTab and slightly updated to fit storing at metadata */
    private JRadioButton saveInOriginalOrder;
    private JRadioButton saveInSpecifiedOrder;

    private final JCheckBox protect = new JCheckBox(Localization.lang("Refuse to save the database before external changes have been reviewed."));
    private boolean oldProtectVal;
    private SaveOrderConfigDisplay saveOrderPanel;


    public DatabasePropertiesDialog(JFrame parent) {
        super(parent, Localization.lang("Database properties"), true);
        encoding = new JComboBox<>();
        encoding.setModel(new DefaultComboBoxModel<>(Encodings.ENCODINGS));
        ok = new JButton(Localization.lang("OK"));
        cancel = new JButton(Localization.lang("Cancel"));
        init(parent);
    }

    public void setPanel(BasePanel panel) {
        this.panel = panel;
        this.metaData = panel.getBibDatabaseContext().getMetaData();
    }

    private void init(JFrame parent) {

        JButton browseFile = new JButton(Localization.lang("Browse"));
        JButton browseFileIndv = new JButton(Localization.lang("Browse"));
        browseFile.addActionListener(BrowseAction.buildForDir(parent, fileDir));
        browseFileIndv.addActionListener(BrowseAction.buildForDir(parent, fileDirIndv));

        setupSortOrderConfiguration();

        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, fill:pref",
                "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"));
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        builder.add(Localization.lang("Database encoding")).xy(1, 1);
        builder.add(encoding).xy(3, 1);

        builder.addSeparator(Localization.lang("Override default file directories")).xyw(1, 3, 5);
        builder.add(Localization.lang("General file directory")).xy(1, 5);
        builder.add(fileDir).xy(3, 5);
        builder.add(browseFile).xy(5, 5);
        builder.add(Localization.lang("User-specific file directory")).xy(1, 7);
        builder.add(fileDirIndv).xy(3, 7);
        builder.add(browseFileIndv).xy(5, 7);

        builder.addSeparator(Localization.lang("Save sort order")).xyw(1, 13, 5);
        builder.add(saveInOriginalOrder).xyw(1, 15, 5);
        builder.add(saveInSpecifiedOrder).xyw(1, 17, 5);

        saveOrderPanel = new SaveOrderConfigDisplay();
        builder.add(saveOrderPanel.getPanel()).xyw(1, 21, 5);

        builder.addSeparator(Localization.lang("Database protection")).xyw(1, 23, 5);
        builder.add(protect).xyw(1, 25, 5);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                storeSettings();
                dispose();
            }
        });

        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    private void setupSortOrderConfiguration() {
        saveInOriginalOrder = new JRadioButton(Localization.lang("Save entries in their original order"));
        saveInSpecifiedOrder = new JRadioButton(Localization.lang("Save entries ordered as specified"));

        ButtonGroup bg = new ButtonGroup();
        bg.add(saveInOriginalOrder);
        bg.add(saveInSpecifiedOrder);
        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = e.getSource() == saveInSpecifiedOrder;
                saveOrderPanel.setEnabled(selected);
            }
        };

        saveInOriginalOrder.addActionListener(listener);
        saveInSpecifiedOrder.addActionListener(listener);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setValues();
        }
        super.setVisible(visible);
    }

    private void setValues() {
        encoding.setSelectedItem(panel.getEncoding());

        List<String> storedSaveOrderConfig = metaData.getData(MetaData.SAVE_ORDER_CONFIG);
        boolean selected;
        if (storedSaveOrderConfig == null) {
            saveInOriginalOrder.setSelected(true);
            oldSaveOrderConfig = null;
            selected = false;
        } else {
            SaveOrderConfig saveOrderConfig;
            saveOrderConfig = new SaveOrderConfig(storedSaveOrderConfig);
            oldSaveOrderConfig = saveOrderConfig;
            if (saveOrderConfig.saveInOriginalOrder) {
                saveInOriginalOrder.setSelected(true);
                selected = false;
            } else {
                assert (saveOrderConfig.saveInSpecifiedOrder);
                saveInSpecifiedOrder.setSelected(true);
                selected = true;
            }
            saveOrderPanel.setSaveOrderConfig(saveOrderConfig);
        }
        saveOrderPanel.setEnabled(selected);

        List<String> fileD = metaData.getData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR));
        if (fileD == null) {
            fileDir.setText("");
        } else {
            // Better be a little careful about how many entries the Vector has:
            if (!(fileD.isEmpty())) {
                fileDir.setText((fileD.get(0)).trim());
            }
        }

        List<String> fileDI = metaData.getData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_INDIVIDUAL)); // File dir setting
        List<String> fileDIL = metaData.getData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_IND_LEGACY)); // Legacy file dir setting for backward comp.
        if (fileDI == null) {
            oldFileIndvVal = fileDirIndv.getText(); // Record individual file dir setting as originally empty if reading from legacy setting
            if (fileDIL == null) {
                fileDirIndv.setText("");
            } else {
                // Insert path from legacy setting if possible
                // Better be a little careful about how many entries the Vector has:
                if (!(fileDIL.isEmpty())) {
                    fileDirIndv.setText((fileDIL.get(0)).trim());
                }
            }
        } else {
            // Better be a little careful about how many entries the Vector has:
            if (!(fileDI.isEmpty())) {
                fileDirIndv.setText((fileDI.get(0)).trim());
            }
            oldFileIndvVal = fileDirIndv.getText(); // Record individual file dir setting normally if reading from ordinary setting
        }

        List<String> prot = metaData.getData(Globals.PROTECTED_FLAG_META);
        if (prot == null) {
            protect.setSelected(false);
        } else {
            if (!(prot.isEmpty())) {
                protect.setSelected(Boolean.parseBoolean(prot.get(0)));
            }
        }

        // Store original values to see if they get changed:
        oldFileVal = fileDir.getText();
        oldProtectVal = protect.isSelected();
    }

    private void storeSettings() {
        SaveOrderConfig newSaveOrderConfig = saveOrderPanel.getSaveOrderConfig();
        if (saveInOriginalOrder.isSelected()) {
            newSaveOrderConfig.setSaveInOriginalOrder();
        } else {
            newSaveOrderConfig.setSaveInSpecifiedOrder();
        }
        Vector<String> serialized = newSaveOrderConfig.getVector();
        metaData.putData(MetaData.SAVE_ORDER_CONFIG, serialized);

        Charset oldEncoding = panel.getEncoding();
        Charset newEncoding = (Charset) encoding.getSelectedItem();
        panel.setEncoding(newEncoding);

        List<String> dir = new ArrayList<>(1);
        String text = fileDir.getText().trim();
        if (text.isEmpty()) {
            metaData.remove(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR));
        } else {
            dir.add(text);
            metaData.putData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR), new Vector<>(dir));
        }
        // Repeat for individual file dir - reuse 'text' and 'dir' vars
        dir = new ArrayList<>(1);
        text = fileDirIndv.getText().trim();
        if (text.isEmpty()) {
            metaData.remove(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_INDIVIDUAL));
        } else {
            dir.add(text);
            metaData.putData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_INDIVIDUAL), new Vector<>(dir));
        }

        if (protect.isSelected()) {
            dir = new ArrayList<>(1);
            dir.add("true");
            metaData.putData(Globals.PROTECTED_FLAG_META, new Vector<>(dir));
        } else {
            metaData.remove(Globals.PROTECTED_FLAG_META);
        }

        // See if any of the values have been modified:
        boolean saveOrderConfigChanged;
        if (oldSaveOrderConfig == newSaveOrderConfig) {
            saveOrderConfigChanged = false;
        } else if ((oldSaveOrderConfig == null) || (newSaveOrderConfig == null)) {
            saveOrderConfigChanged = true;
        } else {
            // check on vector basis. This is slower than directly implementing equals, but faster to implement
            saveOrderConfigChanged = !oldSaveOrderConfig.getVector().equals(newSaveOrderConfig.getVector());
        }

        boolean changed = saveOrderConfigChanged || !newEncoding.equals(oldEncoding)
                || !oldFileVal.equals(fileDir.getText())
                || !oldFileIndvVal.equals(fileDirIndv.getText())
                || (oldProtectVal != protect.isSelected());
        // ... if so, mark base changed. Prevent the Undo button from removing
        // change marking:
        if (changed) {
            panel.markNonUndoableBaseChanged();
        }
    }
}
