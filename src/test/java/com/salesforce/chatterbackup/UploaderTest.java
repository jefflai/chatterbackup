package com.salesforce.chatterbackup;

import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UploaderTest {

	String endpoint;
	String user;
	String pass;

	@BeforeClass
	public void classSetup() throws Exception {
		Properties properties = new Properties();
		properties.load(getClass().getClassLoader().getResourceAsStream(
				"test.properties"));
		endpoint = properties.getProperty("sfdc.endpoint");
		user = properties.getProperty("sfdc.user");
		pass = properties.getProperty("sfdc.password");
	}

	@Test
	public void testLogin() throws Exception {
		Uploader uploader = new Uploader(endpoint);
		uploader.login(user, pass);
		Assert.assertNotNull(uploader.sessionId);
		Assert.assertNotEquals(uploader.sessionId, "");
	}

	@Test
	public void testGetUserId() throws Exception {
		Uploader uploader = new Uploader(endpoint);
		uploader.login(user, pass);
		String id = uploader.getUserId(user);
		Assert.assertNotNull(id);
		Assert.assertNotEquals(id, "");
	}

	@Test
	public void testUploadFileToFeed() throws Exception {
		Uploader uploader = new Uploader(endpoint);
		uploader.login(user, pass);
		String id = uploader.getUserId(user);
		ResponseEntity<String> response = uploader.uploadFileToFeed(id, "test",
				new ClassPathResource("test.txt"), "test.txt", "description");
		Assert.assertEquals(response.getStatusCode().value(), 201);
	}

	@Test
	public void testZipAndUploadDirToFeed() throws Exception {
		Uploader uploader = new Uploader(endpoint);
		uploader.login(user, pass);
		String id = uploader.getUserId(user);
		ResponseEntity<String> response = uploader.zipAndUploadDirToFeed(id,
				"test.zip", new ClassPathResource("testdir"), "test.zip",
				"description");
		Assert.assertEquals(response.getStatusCode().value(), 201);
	}

}
