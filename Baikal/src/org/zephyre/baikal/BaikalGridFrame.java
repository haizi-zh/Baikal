package org.zephyre.baikal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public class BaikalGridFrame extends JFrame {
	private BaikalGridPanel gridPanel;

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

	public void setDrawMarkers(boolean val) {
		gridPanel.drawMarkers = val;
	}

	public void setDrawXGridLines(boolean val) {
		gridPanel.drawXGridLines = val;
	}
	
	public void setDrawYGridLines(boolean val) {
		gridPanel.drawYGridLines = val;
	}

	public void setSegCount(int i) {
		gridPanel.segCount = i;
	}

	public void setXYOffset(int xoffset, int yoffset) {
		gridPanel.xOffset = xoffset;
		gridPanel.yOffset = yoffset;
	}
}
