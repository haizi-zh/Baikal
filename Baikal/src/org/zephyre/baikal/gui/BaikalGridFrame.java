package org.zephyre.baikal.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public class BaikalGridFrame extends JFrame {
	private BaikalGridPanel gridPanel;

	/**
	 * 获得内部的BaikalGridPanel对象。
	 * @return 内部的BaikalGridPanel对象。
	 */
	public BaikalGridPanel getPanel() {
		return gridPanel;
	}

	public BaikalGridFrame() {
		gridPanel = new BaikalGridPanel();
		getContentPane().add(gridPanel);

		setTitle("测试栅格");
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
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
		gridPanel.drawMarkers_ = val;
	}

	public synchronized void drawHorzGridLines(boolean val) {
		gridPanel.drawHorzGridLines_ = val;
	}

	public synchronized void drawVertGridLines(boolean val) {
		gridPanel.drawVertGridLines_ = val;
	}
	
	/**
	 * 是否显示所有的栅格
	 * @param val
	 */
	public void displayFullGrids(boolean val){
		gridPanel.displayFullGrids(val);
	}

	/**
	 * 设置水平及垂直栅格线的offset
	 * @param vertOffset 垂直栅格线的偏移
	 * @param horzOffset 水平栅格线的偏移
	 */
	public void setGridLineOffset(int vertOffset, int horzOffset) {
		gridPanel.vertOffset_ = vertOffset;
		gridPanel.horzOffset_ = horzOffset;
	}
}
