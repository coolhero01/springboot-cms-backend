package com.oneclicktech.spring.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.mapper.AuditLogMapper;
import com.oneclicktech.spring.mapper.CronAuditLogMapper;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.util.DateUtil;

@Service
public class EmailServiceImpl implements EmailService {

	private static final Logger logger = Logger.getLogger("EmailServiceImpl");

	private static final int AUDIT_MSG_CHAR_LIMIT = 3999;

	@Autowired
	CronAuditLogMapper cronAuditLogMapper;

	@Autowired
	AuditLogMapper auditLogMapper;

	@Value("${local.pc.mail.smtp.host}")
	String mailHost;

	@Value("${local.pc.mail.smtp.port}")
	String mailPort;

	@Value("${local.pc.mail.smtp.username}")
	String mailUsername;

	@Value("${local.pc.mail.smtp.password}")
	String mailPassword;
	
	@Value("${pc.cms.environment}")
	String pcCMSEnvironment; 
 
	
	@PostConstruct
	public void init() {

	}

	@Override
	public boolean sendEmail(String emailFrom, String emailPass, String emailAlias, String[] recipients, String subject,
			String emailMsg, String[] emailCC, String replyTo, File attachFile) {

		logger.info("*** EmailServiceImpl >> sendEmail >> [START]");

		Properties prop = new Properties();
		prop.put("mail.smtp.host", mailHost);
		prop.put("mail.smtp.port", mailPort);
		prop.put("mail.smtp.auth", "true");
		prop.put("mail.smtp.socketFactory.port", mailPort);
		prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		prop.put("mail.smtp.connectiontimeout", "30000"); // 30 seconds

		Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailUsername, mailPassword);
			}
		});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailUsername));

			InternetAddress[] addressTo = new InternetAddress[recipients.length];
			for (int i = 0; i < recipients.length; i++) {
				addressTo[i] = new InternetAddress(recipients[i]);
			}

			message.setRecipients(Message.RecipientType.TO, addressTo);

			message.setSubject("Testing Gmail SSL");
			message.setText("Dear Mail Crawler," + "\n\n Please do not spam my email!");

			Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException e) {
			e.printStackTrace();
		}

		logger.info("*** EmailServiceImpl >> sendEmail >> [END]");

		return false;
	}

	@Override
	public boolean sendEmail(String emailAlias, String[] recipients, String subject, String emailMsg, String[] emailCC,
			String replyTo, File attachFile) {

		logger.info("*** EmailServiceImpl >> sendEmail >> [START]");
		boolean isMailSent = false;
		Properties prop = new Properties();
		prop.put("mail.smtp.host", mailHost);
		prop.put("mail.smtp.port", mailPort);
		prop.put("mail.smtp.auth", "true");
		prop.put("mail.smtp.socketFactory.port", mailPort);
		prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		prop.put("mail.smtp.connectiontimeout", "30000"); // 30 seconds

		Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailUsername, mailPassword);
			}
		});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailUsername));

			InternetAddress[] addressTo = new InternetAddress[recipients.length];
			for (int i = 0; i < recipients.length; i++) {
				addressTo[i] = new InternetAddress(recipients[i]);
			}
      
			message.setRecipients(Message.RecipientType.TO, addressTo);
			String newSubject = new StringBuilder("[").append(pcCMSEnvironment).append("] ")
					.append(subject).toString();
					  
			message.setSubject(newSubject);
			message.setText(emailMsg);

			Transport.send(message);

			isMailSent = true;

		} catch (MessagingException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			isMailSent = false;
		}

		logger.info("*** EmailServiceImpl >> sendEmail >> [END]");

		return isMailSent;
	}

	@Override
	public List<HashMap<String, Object>> readGMail(final String subject, final Date fromDate, final Date toDate ) {
		logger.info(" [EmailServiceImpl] >> readGMail >> START");
		Folder inbox = null;
		Store store = null;
		List<HashMap<String, Object>> mailMessages = new ArrayList<>();

		try { 
			Properties prop = new Properties();
			prop.put("mail.smtp.host", mailHost);
			prop.put("mail.smtp.port", mailPort);
			prop.put("mail.smtp.auth", "true"); 
			prop.put("mail.smtp.socketFactory.port", mailPort);
			prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			prop.put("mail.smtp.connectiontimeout", "30000"); // 30 seconds

			Session session = Session.getDefaultInstance(prop, null);
		 
			store = session.getStore("imaps");
			store.connect("smtp.gmail.com", mailUsername, mailPassword);

			inbox = store.getFolder("inbox");

			inbox.open(Folder.READ_ONLY);

			// int messageCount = inbox.getMessageCount();
			// System.out.println("Total Messages:- " + messageCount);
			logger.info(" [EmailServiceImpl] >> readGMail >> fromDate: " + fromDate.toString());
			logger.info(" [EmailServiceImpl] >> readGMail >> toDate: " + toDate.toString());
			SearchTerm greaterThan = new ReceivedDateTerm(ComparisonTerm.GT, fromDate);
			SearchTerm lessThan = new ReceivedDateTerm(ComparisonTerm.LT, toDate);
			// SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT,
			// mailFrom);
			// SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT,
			// mailTo);   
			SearchTerm andTerm = new AndTerm(greaterThan, lessThan);
			Message[] messages = inbox.search(greaterThan);
			logger.info(" [EmailServiceImpl] >> readGMail >> messages: " + messages.length);
			for (Message msg : messages) {
				boolean isValidDate = DateUtil.isWithinDate(fromDate, toDate, msg.getReceivedDate());

				try {

					if (!msg.getSubject().startsWith(subject))
						continue;

					if (!isValidDate)
						continue;

					HashMap<String, Object> msgMap = new HashMap<String, Object>();
					logger.info(" [EmailServiceImpl] >> readGMail >> getReceivedDate: " + msg.getReceivedDate());
					logger.info(" [EmailServiceImpl] >> readGMail >> getSubject: " + msg.getSubject());

					mailMessages.add(msgMap);
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			logger.info(" [EmailServiceImpl] >> readGMail >> MAILMESSAGES ADDED: " + mailMessages.size());
			inbox.close(true);
			store.close();

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			try {
				if (inbox != null)
					inbox.close(true);

				if (store != null)
					store.close();
			} catch (Exception e) {
				// logger.log(Level.WARNING, e.getMessage(), e);
			}
		}
		logger.info(" [EmailServiceImpl] >> readGMail >> END ");
		return mailMessages;
	}

	 

}
