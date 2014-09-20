/*  

Copyright [2013-2014] eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package models.data.providers.ssh;

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program enables you to connect to sshd server and get the shell prompt.
 *   $ CLASSPATH=.:../build javac Shell.java 
 *   $ CLASSPATH=.:../build java Shell
 * You will be asked username, hostname and passwd. 
 * If everything works fine, you will get the shell prompt. Output may
 * be ugly because of lacks of terminal-emulation, but you can issue commands.
 *
 */
import com.jcraft.jsch.*;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.*;

import models.utils.DateUtils;
import models.utils.ErrorMsgUtils;
import models.utils.VarUtils;
/**
 * Run ssh 
 * @author ypei
 *
 */
public class SshProvider {

	public static void main(String[] arg) {
		//testExecuteSshCommand();
	}

	public static void testExecuteSshCommand() {

		String userAtHost = "...";
		String passwd = "****";
		String commandSshLine = "wget -q http://.../packages/discover_os_info.py ; chmod +x discover_os_info.py;  ./discover_os_info.py";

		
		executeSshCommand(userAtHost, passwd, commandSshLine);
	}

	public static SshResponse executeSshCommand(String userAtHost,
			String passwd, String commandSshLine) {

		SshResponse sshResponse = new SshResponse();
		try {
			JSch jsch = new JSch();

			// jsch.setKnownHosts("/home/foo/.ssh/known_hosts");

			String sshCommandLineActual = VarUtils.SSH_COMMAND_PREPROCESS + commandSshLine;

			String user=userAtHost.substring(0, userAtHost.indexOf('@'));
		    String host=userAtHost.substring(userAtHost.indexOf('@')+1);
			
			Session session = jsch.getSession(user, host, 22);

			session.setPassword(passwd);

			UserInfo ui = new MyUserInfo() {
				public void showMessage(String message) {
					JOptionPane.showMessageDialog(null, message);
				}

				public boolean promptYesNo(String message) {
					return true;
				}

			};

			session.setUserInfo(ui);
			session.setConfig("StrictHostKeyChecking", "no");

			// session.connect();

			session.connect(VarUtils.SSH_CONNECTION_TIMEOUT_MILLIS); // making a
															// connection with
															// timeout.

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(sshCommandLineActual);

			// X Forwarding
			// channel.setXForwarding(true);

			// channel.setInputStream(System.in);
			channel.setInputStream(null);

			// channel.setOutputStream(System.out);

			// FileOutputStream fos=new FileOutputStream("/tmp/stderr");

			// ((ChannelExec) channel).setErrStream(System.err);

			channel.connect();
			// change logic jeff.
			sshResponse = executeAndGenResponse(channel);

			if (VarUtils.IN_DETAIL_DEBUG && sshResponse != null) {
				System.out.println(sshResponse.toString());
			}

			channel.disconnect();
			session.disconnect();

		} catch (Throwable t) {
			
			
			
			
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);

			String displayError = ErrorMsgUtils.replaceErrorMsg(t
					.toString());
			
			// 20130522: get details of error message out.
			String detailErrorMsgWithStackTrace = displayError
					+ " Details: " + sw.toString();
			
			sshResponse.setErrorMessage(detailErrorMsgWithStackTrace);
			sshResponse.setError(true);
			
			
		}
		
		return sshResponse;
	}

	/**
	 * Seems there are bad naming in the library
	 * 
	 * the sysout is in channel.getInputStream(); the ssyerr is in
	 * ((ChannelExec)channel).setErrStream(os);
	 * 
	 * @param channel
	 * @return
	 */
	public static SshResponse executeAndGenResponse(Channel channel) {

		InputStream in = null;
		SshResponse sshResponse = new SshResponse();
		OutputStream outputStreamErr = new ByteArrayOutputStream();


		try {

			if (channel != null) {
				in = channel.getInputStream();

				((ChannelExec) channel).setErrStream(outputStreamErr);

			} else {
				
				String errorMsg  = "com.jcraft.jsch.Channel:channel is null in executeAndGenResponse ";
				models.utils.LogUtils.printLogError(errorMsg);
				
				sshResponse.setErrorMessage(errorMsg);
				sshResponse.setError(true);
				return sshResponse;
			}

			if (in == null) {
				
				String errorMsg  = "input stream  is null in executeAndGenResponse ";
				models.utils.LogUtils.printLogError(errorMsg);
				
				sshResponse.setErrorMessage(errorMsg);
				sshResponse.setError(true);
				return sshResponse;
			}

			StringBuilder sb = new StringBuilder();

			byte[] tmp = new byte[VarUtils.SSH_BUFFER_SIZE];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, VarUtils.SSH_BUFFER_SIZE);
					if (i < 0)
						break;
					sb.append(new String(tmp, 0, i));

				}
				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					sshResponse.setError(false);
					sshResponse.setExitStatus(channel.getExitStatus());

					break;
				}

				try {
					Thread.sleep(VarUtils.SSH_SLEEP_MILLIS_BTW_READ_BUFFER);
				} catch (Exception e) {
					models.utils.LogUtils.printLogError(e.getLocalizedMessage()
							+ DateUtils.getNowDateTimeStrSdsm());
				}
			}

			//
			sb.append(outputStreamErr.toString());
			sshResponse.setResponseContent(sb.toString());

		} catch (Throwable t) {
			//t.printStackTrace();
			
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);

			String displayError = ErrorMsgUtils.replaceErrorMsg(t
					.toString());

			// 20130522: get details of error message out.
			String detailErrorMsgWithStackTrace = displayError
					+ " Details: " + sw.toString();
			
			sshResponse.setErrorMessage(detailErrorMsgWithStackTrace);
			sshResponse.setError(true);
			
		}

		return sshResponse;
	}

	public static class SshResponse {

		private String responseContent;
		private String errorMessage;
		private boolean isError;
		private int exitStatus;

		
		
		public SshResponse() {
			super();
			responseContent = VarUtils.NA;
			errorMessage = VarUtils.NA;
			isError = false;
			exitStatus = VarUtils.INVALID_NUM;
		}

		public SshResponse(String responseContent, boolean isError,
				int exitStatus, String errorMessage) {
			super();
			this.responseContent = responseContent;
			this.isError = isError;
			this.exitStatus = exitStatus;
			this.errorMessage = errorMessage;
		}

		public String getResponseContent() {
			return responseContent;
		}

		public void setResponseContent(String responseContent) {
			this.responseContent = responseContent;
		}

		public boolean isError() {
			return isError;
		}

		public void setError(boolean isError) {
			this.isError = isError;
		}

		public int getExitStatus() {
			return exitStatus;
		}

		public void setExitStatus(int exitStatus) {
			this.exitStatus = exitStatus;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		@Override
		public String toString() {
			return "SshResponse [responseContent=" + responseContent
					+ ", errorMessage=" + errorMessage + ", isError=" + isError
					+ ", exitStatus=" + exitStatus + "]";
		}

		

	}

	/**
	 * By original sample
	 * 
	 * @author ypei
	 * 
	 */
	public static abstract class MyUserInfo implements UserInfo,
			UIKeyboardInteractive {
		public String getPassword() {
			return null;
		}

		public boolean promptYesNo(String str) {
			return false;
		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return false;
		}

		public boolean promptPassword(String message) {
			return false;
		}

		public void showMessage(String message) {
		}

		public String[] promptKeyboardInteractive(String destination,
				String name, String instruction, String[] prompt, boolean[] echo) {
			return null;
		}
	}
}
