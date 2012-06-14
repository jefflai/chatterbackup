package com.salesforce.chatterbackup;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.RestClientException;
import org.xml.sax.SAXException;

public class App {

	static String endpoint;
	static String user;
	static String pass;
	static String backupDirPath;
	static String description;

	public static void main(String[] args) throws RestClientException,
			URISyntaxException, ParserConfigurationException, SAXException,
			IOException, ArchiveException {
		endpoint = System.getProperty("sfdc.endpoint");
		checkVariableSet(endpoint, "sfdc.endpoint");
		user = System.getProperty("sfdc.user");
		checkVariableSet(user, "sfdc.user");
		pass = System.getProperty("sfdc.pass");
		checkVariableSet(pass, "sfdc.pass");
		backupDirPath = System.getProperty("backup.dir");
		checkVariableSet(backupDirPath, "backup.dir");
		description = System.getProperty("backup.desc");
		checkVariableSet(description, "backup.desc");

		File backupDir = new File(backupDirPath);
		
		Uploader uploader = new Uploader(endpoint);
		uploader.login(user, pass);
		String id = uploader.getUserId(user);
		uploader.zipAndUploadDirToFeed(id, backupDir.getName(), new FileSystemResource(backupDir), backupDir.getName() + ".zip", description);
	}

	private static void checkVariableSet(String var, String propertyName) {
		if (var == null || var.equals(""))
			throw new RuntimeException(propertyName + " must be set");
	}

}
