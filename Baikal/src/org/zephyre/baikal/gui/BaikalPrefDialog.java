package org.zephyre.baikal.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.BaikalCore.PrefConst;

import com.google.gson.JsonParseException;

public class BaikalPrefDialog extends JDialog {
	private BaikalCore core;

	public BaikalPrefDialog() {
		super();

		core = BaikalMainFrame.getInstance().getCore();
		initUI();
	}

	private void initUI() {
		JPanel basicPanel = new JPanel();
		basicPanel.setLayout(new BoxLayout(basicPanel, BoxLayout.X_AXIS));
		basicPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel devListPanel = createDeviceListPanel();
		basicPanel.add(devListPanel);
		basicPanel.add(Box.createHorizontalStrut(64));

		JPanel controllerPanel = createControllerPanel();
		basicPanel.add(controllerPanel);

		getContentPane().add(basicPanel);

		setTitle("选项");
		setModal(true);
		pack();
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setLocationRelativeTo(BaikalMainFrame.getInstance());
	}

	private JPanel createControllerPanel() {
		JPanel controllerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.gridx = 0;
		gbc.gridy = 0;

		int isoSpeed = ((Number) core.getEntry(PrefConst.ISO_SPEED))
				.intValue();
		JLabel isoLabel = new JLabel(String.format("%s %d", "感光度：ISO ",
				isoSpeed));
		JSlider isoSlider = new JSlider(0, 7);
		isoSlider.setSnapToTicks(true);

		JLabel meterLabel = new JLabel("测光模式：");
		String[] meterModes = new String[] { "矩阵测光/评价测光", "中央重点测光", "点测光" };
		JComboBox meterCombo = new JComboBox(meterModes);

		JLabel shootModeLabel = new JLabel("拍摄模式：");
		String[] shootModes = new String[] { "光圈优先", "快门优先", "手动" };
		JComboBox shootCombo = new JComboBox(shootModes);

		controllerPanel.add(shootModeLabel, gbc);
		gbc.gridx++;
		controllerPanel.add(shootCombo, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		controllerPanel.add(meterLabel, gbc);
		gbc.gridx++;
		controllerPanel.add(meterCombo, gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		controllerPanel.add(isoLabel, gbc);
		gbc.gridy++;
		gbc.gridwidth = 2;
		controllerPanel.add(isoSlider, gbc);
		gbc.gridwidth = 1;

		gbc.gridy++;
		gbc.weighty = 1;
		controllerPanel.add(Box.createVerticalGlue(), gbc);

		return controllerPanel;
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane rootPane = super.createRootPane();
		rootPane.registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setVisible(false);
			}
		}, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
		return rootPane;
	}

	private JPanel createDeviceListPanel() {
		JPanel devListPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.LINE_START;

		JLabel camLabel = new JLabel("相机型号：");
		JScrollPane camListScrollPane = new JScrollPane();
		camListScrollPane.setBorder(BorderFactory.createEtchedBorder());
		camListScrollPane.setPreferredSize(new Dimension(180, 160));
		DefaultListModel<String> camListModel = new DefaultListModel<String>();
		JList<String> camJList = new JList<String>(camListModel);
		camListScrollPane.getViewport().add(camJList);

		gbc.gridy = 0;
		devListPanel.add(camLabel, gbc);
		gbc.gridy++;
		gbc.gridheight = 2;
		devListPanel.add(camListScrollPane, gbc);
		gbc.gridheight = 1;

		JLabel lensLabel = new JLabel("镜头型号：");

		JScrollPane lensListScrollPane = new JScrollPane();
		lensListScrollPane.setBorder(BorderFactory.createEtchedBorder());
		lensListScrollPane.setPreferredSize(new Dimension(180, 240));
		DefaultListModel<String> lensListModel = new DefaultListModel<String>();
		JList<String> lensJList = new JList<String>(lensListModel);
		lensListScrollPane.getViewport().add(lensJList);

		gbc.gridy += 2;
		devListPanel.add(lensLabel, gbc);
		gbc.gridy++;
		gbc.gridheight = 3;
		devListPanel.add(lensListScrollPane, gbc);
		gbc.gridheight = 1;

		// Buttons
		JButton addCamButton = new JButton("添加相机");
		JButton removeCamButton = new JButton("删除相机");
		JButton addLensButton = new JButton("添加镜头");
		JButton removeLensButton = new JButton("删除镜头");
		JButton calLensButton = new JButton("畸变校正");

		gbc.gridx = 1;
		gbc.gridy = 1;
		devListPanel.add(addCamButton, gbc);
		gbc.gridy = 2;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		devListPanel.add(removeCamButton, gbc);
		gbc.weighty = 0;

		gbc.gridy = 4;
		devListPanel.add(addLensButton, gbc);
		gbc.gridy = 5;
		devListPanel.add(removeLensButton, gbc);
		gbc.gridy = 6;
		gbc.weighty = 1;
		devListPanel.add(calLensButton, gbc);

		HashMap<String, String[]> spDevices = core.getDeviceList();
		String[] camNames = spDevices.get(PrefConst.CAMERA_LIST);
		for (String cam : camNames) {
			camListModel.addElement(cam);
		}
		camJList.setSelectedValue(core.getEntry(PrefConst.CAMERA_MODEL),
				true);

		String[] lensNames = spDevices.get(PrefConst.LENS_LIST);
		for (String lens : lensNames) {
			lensListModel.addElement(lens);
		}
		lensJList.setSelectedValue(core.getEntry(PrefConst.LENS_MODEL),
				true);

		return devListPanel;
	}
}
