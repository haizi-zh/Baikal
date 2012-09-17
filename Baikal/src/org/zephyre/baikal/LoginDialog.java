package org.zephyre.baikal;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
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

public class LoginDialog extends JDialog {
	public String selectedUserName = null;
	public JSONObject userData = null;
	private JList userListJList;

	// private JList userListPane;

	public LoginDialog() {
		initUI(getUserList());
	}

	private void onOkButton() {
		int indx = userListJList.getSelectedIndex();
		if (indx == -1) {
			JOptionPane.showMessageDialog(LoginDialog.this, "请选择用户", "",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		DefaultListModel model = (DefaultListModel) userListJList.getModel();
		selectedUserName = (String) model.get(indx);
		setVisible(false);
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
		DefaultListModel userListModel = new DefaultListModel();
		for (String name : userList) {
			userListModel.addElement(name);
		}
		userListJList = new JList(userListModel);
		if (userListModel.getSize() > 0)
			userListJList.setSelectedIndex(0);
		userListScrollPane.getViewport().add(userListJList);
		basicPanel.add(userListScrollPane);

		basicPanel.add(Box.createRigidArea(new Dimension(10, 0)));

		// Init the button box.
		JButton okButton = new JButton("确定");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onOkButton();
			}
		});
		okButton.setMaximumSize(new Dimension(256,
				okButton.getMaximumSize().height));
		JButton cancelButton = new JButton("取消");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				selectedUserName = null;
				setVisible(false);
			}
		});
		cancelButton.setMaximumSize(new Dimension(256, cancelButton
				.getMaximumSize().height));
		JButton newUserButton = new JButton("新建用户");
		newUserButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String name = JOptionPane.showInputDialog(LoginDialog.this,
						"用户名称", "新建用户", JOptionPane.INFORMATION_MESSAGE);
				if (name != null)
					name = name.trim();
				if (name == null || name.equals(""))
					return;

				// Set up the new user
				JSONObject userData = new JSONObject();
				userData.put("UUID", System.currentTimeMillis());
				userData.put("Name", name);
				userData.put("MirrorId", 1);
				try {
					// If the user exists
					String fileName = name + ".ini";
					if ((new File(fileName)).exists()) {
						throw new IOException();
					}

					BufferedWriter writer = new BufferedWriter(new FileWriter(
							fileName));
					writer.write(userData.toJSONString());
					writer.close();

					// Add to the user list
					DefaultListModel model = (DefaultListModel) userListJList
							.getModel();
					model.addElement(name);
					userListJList.setSelectedIndex(model.getSize() - 1);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(LoginDialog.this, "新建用户失败",
							"", JOptionPane.ERROR_MESSAGE);
				}

			}
		});
		newUserButton.setMaximumSize(new Dimension(256, newUserButton
				.getMaximumSize().height));
		JButton removeUserButton = new JButton("删除用户");
		removeUserButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int indx = userListJList.getSelectedIndex();
				// No selection
				if (indx == -1)
					return;

				DefaultListModel model = (DefaultListModel) userListJList
						.getModel();
				String fileName = (String) model.get(indx) + ".ini";
				File file = new File(fileName);
				boolean success = file.delete();
				if (success)
					model.remove(indx);
				else
					JOptionPane.showMessageDialog(LoginDialog.this, "删除用户失败",
							"", JOptionPane.ERROR_MESSAGE);
			}
		});
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
		setTitle("操作用户登录");
		setLocationRelativeTo(null);
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane rootPane = super.createRootPane();
		rootPane.registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				LoginDialog.this.setVisible(false);
			}
		}, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
		rootPane.registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onOkButton();
			}
		}, KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_IN_FOCUSED_WINDOW);
		return rootPane;
	}

	/**
	 * Get a list of all user names by listing *.ini files in the working
	 * directory.
	 * 
	 * @return List of user names.
	 */
	public String[] getUserList() {

		File folder = new File(".");
		File[] fileList = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				Pattern p = Pattern.compile(".*\\.ini$");
				return p.matcher(name).matches();
			}
		});
		String[] userList = new String[fileList.length];
		for (int i = 0; i < userList.length; i++) {
			String fullName = fileList[i].getName();
			int pos = fullName.lastIndexOf('.');
			if (pos != -1)
				userList[i] = fullName.substring(0, pos);
			else
				userList[i] = fullName;
		}
		return userList;
	}
}
