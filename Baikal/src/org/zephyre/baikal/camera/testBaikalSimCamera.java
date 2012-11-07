/**
 * 
 */
package org.zephyre.baikal.camera;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zephyre.baikal.camera.BaikalCameraException.BaikalCameraErrorDesc;

/**
 * @author Zephyre
 * 
 */
public class testBaikalSimCamera {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private BaikalSimCamera cam_;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cam_ = BaikalSimCamera.getInstance();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (cam_.isConnected())
			cam_.disconnect();
	}

	/**
	 * Test method for
	 * {@link org.zephyre.baikal.camera.BaikalSimCamera#connect()}.
	 */
	@Test
	public void testConnect() {
		try {
			cam_.connect();
			assertTrue(cam_.isConnected());
			cam_.connect();
			assertTrue(cam_.isConnected());
			cam_.disconnect();
			assertFalse(cam_.isConnected());
			cam_.disconnect();
			assertFalse(cam_.isConnected());
			cam_.connect();
		} catch (BaikalCameraException e) {
			fail(e.toString());
		}
	}

	/**
	 * Test method for
	 * {@link org.zephyre.baikal.camera.BaikalSimCamera#getImageByteSize()}.
	 */
	@Test
	public void testGetImageByteSize() {
		try {
			cam_.disconnect();
			cam_.getImageByteSize();
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}

		try {
			cam_.connect();
			assertEquals("The returned image byte size is wrong.",
					cam_.getImageByteSize(), cam_.getWidth() * cam_.getHeight()
							* cam_.getBitDepth() / 8);
		} catch (BaikalCameraException e) {
			fail(e.toString());
		}
	}

	/**
	 * Test method for
	 * {@link org.zephyre.baikal.camera.BaikalSimCamera#getBitDepth()}.
	 */
	@Test
	public void testGetBitDepth() {
		try {
			cam_.disconnect();
			cam_.getBitDepth();
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}
		
		try {
			cam_.connect();
			int depth = cam_.getBitDepth();
			assertTrue(depth > 0);
		} catch (BaikalCameraException e) {
			fail(e.toString());
		}
	}

	/**
	 * Test method for
	 * {@link org.zephyre.baikal.camera.BaikalSimCamera#snapshotAndWait()}.
	 */
	@Test
	public void testSnapshotAndWait() {
		try {
			cam_.disconnect();
			cam_.snapshotAndWait();
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
		} catch (InterruptedException e) {
			fail(e.toString());
		}

		try {
			cam_.disconnect();
			cam_.releaseImage(null);
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}

		try {
			cam_.connect();
			// 拍照但是不归还
			for (int i = 0; i < 10; i++) {
				cam_.snapshotAndWait();
			}
			fail("Buffer's not underflow.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.BUFFER_UNDER_FLOW)
				fail(e.toString());
			cam_.disconnect();
		} catch (InterruptedException e) {
			fail(e.toString());
		}

		// 拍照并归还
		try {
			cam_.connect();
			for (int i = 0; i < 10; i++) {
				BufferedImage bi = cam_.snapshotAndWait();
				cam_.releaseImage(bi);
			}
		} catch (BaikalCameraException e) {
			fail(e.toString());
		} catch (InterruptedException e) {
			fail(e.toString());
		}

		// 归还错误的image
		BufferedImage bi = null;
		try {
			bi = cam_.snapshotAndWait();
			cam_.releaseImage(new BufferedImage(400, 320,
					BufferedImage.TYPE_3BYTE_BGR));
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.INVALID_ARGUMENT)
				fail("The released image must have been registered.");
		} catch (InterruptedException e) {
			fail(e.toString());
		} finally {
			try {
				cam_.releaseImage(bi);
			} catch (BaikalCameraException e) {
			}
		}
	}

	/**
	 * Test method for
	 * {@link org.zephyre.baikal.camera.BaikalSimCamera#getExposureMs()}.
	 */
	@Test
	public void testExposureMs() {
		try {
			cam_.disconnect();
			cam_.getExposureMs();
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}

		try {
			cam_.connect();
			assertTrue(cam_.getExposureMs() > 0);
		} catch (BaikalCameraException e) {
			fail(e.toString());
		}
	}

	/**
	 * Test method for
	 * {@link org.zephyre.baikal.camera.BaikalSimCamera#getInstance()}.
	 */
	@Test
	public void testGetInstance() {
		try {
			BaikalSimCamera cam = BaikalSimCamera.getInstance();
			assertSame("The two instances are not the same.", cam, cam_);
		} catch (BaikalCameraException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.zephyre.baikal.camera.BaikalAbstractCamera#getResolutionList()}
	 * .
	 */
	@Test
	public void testResolutionList() {
		cam_.disconnect();
		try {
			cam_.getResolutionList();
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}
		
		try {
			cam_.setResolution(null);
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}
		try {
			cam_.getWidth();
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}
		try {
			cam_.getHeight();
			fail("No exception thrown while the camera's disconnected.");
		} catch (BaikalCameraException e) {
			if (e.getDescription() != BaikalCameraErrorDesc.CAMERA_NOT_CONNECTED)
				fail("No exception thrown while the camera's disconnected.");
		}

		try {
			cam_.connect();
			// 测试返回的支持分辨率列表
			HashMap<Object, ArrayList<Integer>> res = cam_.getResolutionList();
			assertTrue("Resolution list is empty.", res.size() > 0);
			Iterator<Object> it = res.keySet().iterator();
			while (it.hasNext()) {
				ArrayList<Integer> list = res.get(it.next());
				assertEquals("Resolution must be a pair of integers.",
						list.size(), 2);
				for (int i = 0; i < list.size(); i++)
					assertTrue("Width/height must be greater than zero.",
							list.get(i) > 0);
				assertEquals("Width is odd.", list.get(0) % 2, 0);
			}

			// 检查分辨率
			it = res.keySet().iterator();
			while (it.hasNext()) {
				Object key = it.next();
				ArrayList<Integer> list = res.get(key);

				cam_.setResolution(key);
				int width = cam_.getWidth();
				int height = cam_.getHeight();

				assertEquals("Width not set.", width, list.get(0).intValue());
				assertEquals("Height not set.", height, list.get(1).intValue());
			}
		} catch (BaikalCameraException e) {
			fail(e.toString());
		}

	}
}
