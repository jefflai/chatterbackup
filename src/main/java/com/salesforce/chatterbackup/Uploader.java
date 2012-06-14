package com.salesforce.chatterbackup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Uploader {

	protected String sessionId;
	String sfdcEndpoint;
	String user;
	String pass;

	RestTemplate restTemplate;
	ObjectMapper mapper;
	Logger logger = Logger.getLogger(Uploader.class);

	public Uploader(String sfdcEndpoint) {
		this.sfdcEndpoint = sfdcEndpoint;
		restTemplate = new RestTemplate();
		mapper = new ObjectMapper();
	}

	public void login(String user, String pass) throws RestClientException,
			URISyntaxException, ParserConfigurationException, SAXException,
			IOException {
		String soapMsg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<env:Envelope xmlns:wsdl=\"urn:partner.soap.sforce.com\" "
				+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
				+ "xmlns:ins0=\"urn:partner.soap.sforce.com\" "
				+ "xmlns:ins1=\"urn:fault.partner.soap.sforce.com\" "
				+ "xmlns:ins2=\"urn:sobject.partner.soap.sforce.com\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+ "<env:Body>" + "<ins0:login>" + "<ins0:username>" + user
				+ "</ins0:username>" + "<ins0:password>" + pass
				+ "</ins0:password>" + "</ins0:login>" + "</env:Body>"
				+ "</env:Envelope>";

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-type", "text/xml;charset=UTF-8");
		headers.set("SOAPAction", "\"login\"");

		HttpEntity<String> soapEntity = new HttpEntity<String>(soapMsg, headers);
		logger.info("logging into sfdc as " + user + " on " + sfdcEndpoint);
		String loginResponse = restTemplate.postForObject(new URI(sfdcEndpoint
				+ "/services/Soap/u/25.0"), soapEntity, String.class);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document doc = builder.parse(new ByteArrayInputStream(loginResponse
				.getBytes()));
		sessionId = doc.getElementsByTagName("sessionId").item(0)
				.getTextContent();
	}

	protected void addAuthHeader(HttpHeaders headers) {
		headers.add("Authorization", "OAuth " + sessionId);
	}

	public String getUserId(String user) throws RestClientException,
			URISyntaxException, JsonParseException, JsonMappingException,
			IOException {
		String url = sfdcEndpoint
				+ "/services/data/v25.0/query?q=select+id+from+user+where+username+%3D+'"
				+ user + "'";
		HttpHeaders headers = new HttpHeaders();
		addAuthHeader(headers);
		HttpEntity<Object> entity = new HttpEntity<Object>(headers);
		ResponseEntity<String> response = restTemplate.exchange(new URI(url),
				HttpMethod.GET, entity, String.class);
		JsonNode node = mapper.readValue(response.getBody(), JsonNode.class);
		return node.findPath("records").get(0).get("Id").getTextValue();
	}

	public ResponseEntity<String> uploadFileToFeed(String userId, String title,
			Resource file, String fileName, String description)
			throws IOException, RestClientException, URISyntaxException {
		Map<String, String> map = new HashMap<String, String>();
		map.put("Type", "ContentPost");
		map.put("ParentId", userId);
		map.put("Title", title);
		map.put("ContentFileName", fileName);
		map.put("ContentData", Base64.encodeBase64String(IOUtils
				.toByteArray(file.getInputStream())));
		map.put("ContentDescription", description);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		addAuthHeader(headers);
		String url = sfdcEndpoint + "/services/data/v25.0/sobjects/FeedItem";
		String json = mapper.writeValueAsString(map);
		HttpEntity<String> entity = new HttpEntity<String>(json, headers);
		logger.info("creating feed item " + title);
		ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
		return response;
	}

	public ResponseEntity<String> zipAndUploadDirToFeed(String userId,
			String title, Resource file, String fileName, String description)
			throws IOException, ArchiveException, RestClientException,
			URISyntaxException {
		if (!file.getFile().isDirectory())
			throw new IOException("resource file must be directory");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ArchiveOutputStream os = new ArchiveStreamFactory()
				.createArchiveOutputStream(ArchiveStreamFactory.ZIP, out);
		zipHelper("", file.getFile(), os);
		os.close();
		Resource resource = new ByteArrayResource(out.toByteArray());
		return uploadFileToFeed(userId, title, resource, fileName, description);
	}

	private ArchiveOutputStream zipHelper(String parentPath, File dir,
			ArchiveOutputStream os) throws IOException {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				zipHelper(parentPath + "/" + file.getName(), file, os);
			} else {
				String zipEntryPath = parentPath + "/" + file.getName();
				logger.info("adding to zip archive: " + zipEntryPath);
				os.putArchiveEntry(new ZipArchiveEntry(zipEntryPath));
				IOUtils.copy(new FileInputStream(file), os);
				os.closeArchiveEntry();
			}
		}
		return os;
	}

}
