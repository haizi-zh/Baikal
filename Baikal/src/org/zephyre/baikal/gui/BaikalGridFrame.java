package org.zephyre.baikal.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.zephyre.baikal.BaikalCore;
import org.zephyre.baikal.BaikalRes;
import org.zephyre.baikal.BaikalCore.PrefConst;

public class BaikalGridFrame extends JFrame {
	private BaikalGridPanel gridPanel_;
	private ResourceBundle res_;

	/**
	 * 获得内部的BaikalGridPanel对象。
	 * 
	 * @return 内部的BaikalGridPanel对象。
	 */
	public BaikalGridPanel getPanel() {
		return gridPanel_;
	}

	public BaikalGridFrame() {
		res_ = ResourceBundle.getBundle(BaikalRes.class.getName());
		JPanel basicPanel = new JPanel();
		basicPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.weighty = 1;
		gbc.weightx = 1;

		gridPanel_ = new BaikalGridPanel();
		gridPanel_.setAlignmentX(LEFT_ALIGNMENT);
		gridPanel_.setBorder(BorderFactory.createEtchedBorder());
		basicPanel.add(gridPanel_, gbc);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		JButton saveButton = new JButton(
				res_.getString("GridLinesFrameSavePos"));
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// 移动到预定的位置
				Point location = getLocation();
				Dimension size = gridPanel_.getSize();
				ArrayList<Integer> posSize = new ArrayList<Integer>();
				int[] vals = new int[] { location.x, location.y, size.width,
						size.height };
				for (int v : vals)
					posSize.add(Integer.valueOf(v));
				BaikalCore core = BaikalCore.getInstance();
				core.putEntry(PrefConst.GRID_FRAME_POS_SIZE, posSize);
			}
		});
		JButton restoreButton = new JButton(
				res_.getString("GridLinesFrameRestorePos"));
		restoreButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// 移动到预定的位置
				updatePosSize();
			}
		});
		buttonPanel.add(saveButton);
		final int BUTTON_GAP = 12;
		buttonPanel.add(Box.createHorizontalStrut(BUTTON_GAP));
		buttonPanel.add(restoreButton);
		gbc.gridy = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.NONE;
		final int GAP = 4;
		gbc.insets = new Insets(GAP, GAP, GAP, GAP);
		basicPanel.add(buttonPanel, gbc);

		getContentPane().add(basicPanel);

		setTitle(res_.getString("GridLinesFrameTitle"));
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}

	// 设置位置和大小
	public void updatePosSize(int x, int y, int width, int height) {
		gridPanel_.setPreferredSize(new Dimension(width, height));
		gridPanel_.setSize(width, height);
		setLocation(x, y);
		pack();
		pack();
	}

	public void updatePosSize() {
		BaikalCore core = BaikalCore.getInstance();
		ArrayList<Number> posSize = (ArrayList<Number>) core
				.getEntry(PrefConst.GRID_FRAME_POS_SIZE);
		int width = posSize.get(2).intValue();
		int height = posSize.get(3).intValue();
		BaikalCore.log(String.format("Restore: %d, %d", width, height));
		updatePosSize(posSize.get(0).intValue(), posSize.get(1).intValue(),
				posSize.get(2).intValue(), posSize.get(3).intValue());
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane rootPane = super.createRootPane();
		rootPane.registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				BaikalGridFrame.this.setVisible(false);
			}
		}, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
		return rootPane;
	}

	public synchronized void setDrawMarkers(boolean val) {
		gridPanel_.drawMarkers_ = val;
	}

	public synchronized void drawHorzGridLines(boolean val) {
		gridPanel_.drawHorzGridLines_ = val;
	}

	public synchronized void drawVertGridLines(boolean val) {
		gridPanel_.drawVertGridLines_ = val;
	}

	/**
	 * 是否显示所有的栅格
	 * 
	 * @param val
	 */
	public void displayFullGrids(boolean val) {
		gridPanel_.displayFullGrids(val);
	}

	/**
	 * 设置水平及垂直栅格线的offset
	 * 
	 * @param vertOffset
	 *            垂直栅格线的偏移
	 * @param horzOffset
	 *            水平栅格线的偏移
	 */
	public void setGridLineOffset(int vertOffset, int horzOffset) {
		gridPanel_.vertOffset_ = vertOffset;
		gridPanel_.horzOffset_ = horzOffset;
	}
}
