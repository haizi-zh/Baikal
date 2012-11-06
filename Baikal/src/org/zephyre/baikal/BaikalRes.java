package org.zephyre.baikal;

import java.util.ListResourceBundle;

public class BaikalRes extends ListResourceBundle {
	static final Object[][] contents_ = new String[][] {
			{ "OK", "OK" },
			{ "Cancel", "Cancel" },
			{ "Error", "Error" },
			{ "AppName", "Baikal System" },
			// Login
			{ "NewUser", "New User" }, { "RemoveUser", "Remove" },
			{ "SelectUser", "Please select a user." },
			{ "UserName", "User Name" }, { "UserExist1", "User" },
			{ "UserExist2", "exists." },
			{ "NewUserError", "Failed to add a new user." },
			{ "RemoveUserError", "Failed to remove the user." },
			{ "LoginTitle", "Login" },
			// Image processing
			{ "MarkerLoc", "Localization" }, { "AutoPreview", "Auto preview" },
			// ProcParam
			{"ThresholdParam","Threshold (0~255):"},
			// Main frame
			{ "RunTest", "Run Test" }, };

	@Override
	protected Object[][] getContents() {
		return contents_;
	}

}
