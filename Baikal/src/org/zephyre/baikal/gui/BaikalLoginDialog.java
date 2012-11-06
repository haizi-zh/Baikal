package org.zephyre.baikal.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.BaikalCore.BaikalUserException;
import org.zephyre.baikal.BaikalRes;

public class BaikalLoginDialog extends JDialog {
	public String selectedUserName = null;
	public JSONObject userData = null;
	private JList<String> userListJList;
	private ResourceBundle res_;

	public BaikalLoginDialog() {
		res_ = ResourceBundle.getBundle(BaikalRes.class.getName());
		initUI(BaikalCore.getUserNames());
	}

	private void initUI(String[] userList) {
		setModal(true);
		setResizable(false);

		JPanel basicPanel = new JPanel();
		getContentPane().add(basicPanel);

		basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.X_AXIS));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Init the user list pane.
		JScrollPane userListScrollPane = new JScrollPane();
		userListScrollPane.setBorder(BorderFactory.createEtchedBorder());
		userListScrollPane.setPreferredSize(new Dimension(120, 240));
		DefaultListModel<String> userListModel = new DefaultListModel<String>();
		for (String name : userList) {
			userListModel.addElement(name);
		}
		userListJList = new JList<String>(userListModel);
		if (userListModel.getSize() > 0)
			userListJList.setSelectedIndex(0);
		userListScrollPane.getViewport().add(userListJList);
		basicPanel.add(userListScrollPane);

		basicPanel.add(Box.createRigidArea(new Dimension(10, 0)));

		// Init the button box.
		AbstractAction okAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int indx = userListJList.getSelectedIndex();
				if (indx == -1) {
					JOptionPane.showMessageDialog(BaikalLoginDialog.this,
							res_.getString("SelectUser"),
							res_.getString("Error"), JOptionPane.ERROR_MESSAGE);
					return;
				}
				DefaultListModel<String> model = (DefaultListModel<String>) userListJList
						.getModel();
				selectedUserName = model.get(indx);
				setVisible(false);
			}
		};
		okAction.putValue(AbstractAction.NAME, res_.getString("OK"));
		JButton okButton = new JButton(okAction);
		okButton.setMaximumSize(new Dimension(256,
				okButton.getMaximumSize().height));

		AbstractAction cancelAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedUserName = null;
				setVisible(false);
			}
		};
		cancelAction.putValue(AbstractAction.NAME, res_.getString("Cancel"));
		JButton cancelButton = new JButton(cancelAction);
		cancelButton.setMaximumSize(new Dimension(256, cancelButton
				.getMaximumSize().height));

		AbstractAction newUserAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = JOptionPane.showInputDialog(
						BaikalLoginDialog.this, res_.getString("UserName"),
						res_.getString("NewUser"),
						JOptionPane.INFORMATION_MESSAGE);
				if (name != null)
					name = name.trim();
				if (name == null || name.equals(""))
					return;

				// 新建用户
				try {
					BaikalCore.addUser(name);
					// Add to the user list
					DefaultListModel<String> model = (DefaultListModel<String>) userListJList
							.getModel();
					model.addElement(name);
					userListJList.setSelectedIndex(model.getSize() - 1);
				} catch (BaikalUserException e1) {
					JOptionPane.showMessageDialog(
							BaikalLoginDialog.this,
							String.format("%s%s%s",
									res_.getString("UserExist1"), name,
									res_.getString("UserExist2")),
							res_.getString("Error"), JOptionPane.ERROR_MESSAGE);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(BaikalLoginDialog.this,
							res_.getString("NewUserError"),
							res_.getString("Error"), JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		newUserAction.putValue(AbstractAction.NAME, res_.getString("NewUser"));
		JButton newUserButton = new JButton(newUserAction);
		newUserButton.setMaximumSize(new Dimension(256, newUserButton
				.getMaximumSize().height));

		AbstractAction removeUserAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int indx = userListJList.getSelectedIndex();
				// No selection
				if (indx == -1)
					return;

				DefaultListModel<String> model = (DefaultListModel<String>) userListJList
						.getModel();
				String userName = model.get(indx);
				try {
					BaikalCore.removeUser(userName);
				} catch (BaikalUserException e1) {
					JOptionPane.showMessageDialog(BaikalLoginDialog.this,
							res_.getString("RemoveUserError"), res_.getString("Error"), JOptionPane.ERROR_MESSAGE);
				}
				model.remove(indx);
			}
		};
		removeUserAction.putValue(AbstractAction.NAME,
				res_.getString("RemoveUser"));
		JButton removeUserButton = new JButton(removeUserAction);
		removeUserButton.setMaximumSize(new Dimension(256, removeUserButton
				.getMaximumSize().height));

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		JPanel buttonBox = new JPanel();
		buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.Y_AXIS));

		buttonBox.add(newUserButton);
		buttonBox.add(Box.createVerticalStrut(10));
		buttonBox.add(removeUserButton);
		buttonBox.add(Box.createVerticalGlue());
		buttonBox.add(okButton);
		buttonBox.add(Box.createVerticalStrut(10));
		buttonBox.add(cancelButton);

		rightPanel.add(buttonBox);
		rightPanel.add(Box.createVerticalGlue());

		basicPanel.add(buttonBox);

		pack();
		setTitle(res_.getString("LoginTitle"));
		setLocationRelativeTo(null);

		// 键盘事件
		ActionMap amap = getRootPane().getActionMap();
		InputMap imap = getRootPane().getInputMap(
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				"rootpane.escape");
		amap.put("rootpane.escape", cancelAction);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "rootpane.enter");
		amap.put("rootpane.enter", okAction);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N,
				InputEvent.CTRL_DOWN_MASK), "rootpane.new");
		amap.put("rootpane.new", newUserAction);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D,
				InputEvent.CTRL_DOWN_MASK), "rootpane.remove");
		amap.put("rootpane.remove", removeUserAction);
	}
}
