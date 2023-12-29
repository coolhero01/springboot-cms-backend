package com.oneclicktech.spring.service;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface EmailService {
	
	public boolean sendEmail(String emailFrom, String emailPass,String emailAlias, String[] recipients, 
			String subject, String message, String[] emailCC, String replyTo,  File attachFile);
	
	public boolean sendEmail(String emailAlias, String[] recipients, 
			String subject, String message, String[] emailCC, String replyTo,  File attachFile);
	
	
	public List<HashMap<String, Object>> readGMail(final  String subject
			, final  Date fromDate, final Date toDate );
}
