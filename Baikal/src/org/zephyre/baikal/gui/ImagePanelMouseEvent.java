/**
 * 
 */
package org.zephyre.baikal.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * @author Zephyre
 * 
 */
public class ImagePanelMouseEvent extends MouseEvent {

	private int imgX;
	private int imgY;

	/**
	 * @param source
	 * @param id
	 * @param when
	 * @param modifiers
	 * @param x
	 * @param y
	 * @param clickCount
	 * @param popupTrigger
	 */
	public ImagePanelMouseEvent(Component source, int id, long when,
			int modifiers, int x, int y, int clickCount, boolean popupTrigger) {
		super(source, id, when, modifiers, x, y, clickCount, popupTrigger);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param source
	 * @param id
	 * @param when
	 * @param modifiers
	 * @param x
	 * @param y
	 * @param clickCount
	 * @param popupTrigger
	 * @param button
	 */
	public ImagePanelMouseEvent(Component source, int id, long when,
			int modifiers, int x, int y, int clickCount, boolean popupTrigger,
			int button) {
		super(source, id, when, modifiers, x, y, clickCount, popupTrigger,
				button);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param source
	 * @param id
	 * @param when
	 * @param modifiers
	 * @param x
	 * @param y
	 * @param xAbs
	 * @param yAbs
	 * @param clickCount
	 * @param popupTrigger
	 * @param button
	 */
	public ImagePanelMouseEvent(Component source, int id, long when,
			int modifiers, int x, int y, int xAbs, int yAbs, int clickCount,
			boolean popupTrigger, int button) {
		super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount,
				popupTrigger, button);
		// TODO Auto-generated constructor stub
	}

	public ImagePanelMouseEvent(MouseEvent evt) {
		super((Component) evt.getSource(), evt.getID(), evt.getWhen(), evt
				.getModifiers(), evt.getX(), evt.getY(), evt.getClickCount(),
				evt.isPopupTrigger());
	}

	public void setImageCoordinates(int x, int y) {
		imgX = x;
		imgY = y;
	}

	public int getImageX() {
		return imgX;
	}

	public int getImageY() {
		return imgY;
	}
}
