package org.zephyre.baikal;

import java.util.ListResourceBundle;

public class BaikalRes_zh_CN extends ListResourceBundle {
	static final Object[][] contents_ = new String[][] {
			{ "OK", "确定" },
			{ "Cancel", "取消" },
			{ "Error", "错误" },
			{ "AppName", "Baikal光学检测系统" },
			{ "CameraError","相机错误。"},
			// Login
			{ "NewUser", "新建用户" },
			{ "RemoveUser", "删除用户" },
			{ "SelectUser", "请选择用户。" },
			{ "UserName", "用户名称" },
			{ "UserExist1", "用户名" },
			{ "UserExist2", "已经存在" },
			{ "NewUserError", "新建用户出错。" },
			{ "RemoveUserError", "删除用户出错。" },
			{ "LoginTitle", "用户登录" },
			// Image processing
			{ "MarkerLoc", "定位" },
			{ "AutoPreview", "自动预览" },
			// ProcParam
			{ "ThresholdParam", "阈值 (0~255)" },
			{ "SmoothWinSize", "平滑窗口大小" },
			{ "SmoothParam", "平滑参数" },
			{ "HoughDp", "DP" },
			{ "HoughMinDist", "圆最小间距" },
			{ "HoughCannyThreshold", "Canny阈值" },
			{ "HoughAccThreshold", "Accumulator阈值" },
			{ "HoughMinRadius", "最小半径" },
			{ "HoughMaxRadius", "最大半径" },
			// 测量过程
			{ "AcqError", "相机错误，图像采集失败。是否重试？" },
			{ "AcqSucceeded", "是否接受这张图像？" },
			{ "ReadyToShot", "请释放快门，然后点击“确定”按钮。" },
			// Main frame
			{ "RunTest", "开始测试" }, { "SetParam", "参数设置" },{"SnapShotAction","单张拍摄"},
			{ "ToggleAllAction", "全部选择/全部取消" }, { "Toggle1", "红" },
			{ "Toggle2", "绿" }, { "Toggle3", "蓝" } };

	@Override
	protected Object[][] getContents() {
		return contents_;
	}
}
