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
package notifiers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.utils.ConfUtils;
import models.utils.DateUtils;
import models.utils.VarUtils;

import play.mvc.Mailer;

public class MailNotifier extends Mailer {

	/**
	 * using a default sender
	 * 
	 * @param emailRecipients
	 * @param emailContent
	 * @param emailTitle
	 */
	public static void sendEmailGeneric(List<String> emailRecipients,
			String emailContent, String emailTitle, String emailContentHeader, String emailContentSpecialNotes, String lastRefreshed) {


		if(lastRefreshed==null){
			lastRefreshed = DateUtils.getNowDateTimeStrSdsm();
		}

		setSubject(emailTitle);
		for (String email : emailRecipients) {
			addRecipient(email);
		}

		setFrom(VarUtils.EMAIL_SOURCE_ADDRESS1);
		send(emailContent, emailContentHeader, emailContentSpecialNotes, lastRefreshed);

	}// end func.

	

}