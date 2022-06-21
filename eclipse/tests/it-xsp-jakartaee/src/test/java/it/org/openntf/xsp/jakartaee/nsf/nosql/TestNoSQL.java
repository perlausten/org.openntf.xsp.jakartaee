/**
 * Copyright © 2018-2022 Contributors to the XPages Jakarta EE Support Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.org.openntf.xsp.jakartaee.nsf.nosql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.ibm.commons.util.io.json.JsonException;
import com.ibm.commons.util.io.json.JsonJavaFactory;
import com.ibm.commons.util.io.json.JsonParser;

import it.org.openntf.xsp.jakartaee.AbstractWebClientTest;

public class TestNoSQL extends AbstractWebClientTest {
	@SuppressWarnings("unchecked")
	@Test
	public void testNoSql() throws JsonException {
		Client client = getAnonymousClient();
		WebTarget target = client.target(getRestUrl(null) + "/nosql?lastName=CreatedUnitTest"); //$NON-NLS-1$
		
		{
			Response response = target.request().get();
			
			String json = response.readEntity(String.class);
			Map<String, Object> jsonObject = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, json);
			
			List<Object> byQueryLastName = (List<Object>)jsonObject.get("byQueryLastName"); //$NON-NLS-1$
			assertNotNull(byQueryLastName, () -> String.valueOf(jsonObject));
			assertTrue(byQueryLastName.isEmpty(), () -> String.valueOf(jsonObject));
		}
		
		// Now use the MVC endpoint to create one, which admittedly is outside this test
		{
			MultivaluedMap<String, String> payload = new MultivaluedHashMap<>();
			payload.putSingle("firstName", "foo"); //$NON-NLS-1$ //$NON-NLS-2$
			payload.putSingle("lastName", "CreatedUnitTest"); //$NON-NLS-1$ //$NON-NLS-2$
			payload.putSingle("customProperty", "i am custom property"); //$NON-NLS-1$ //$NON-NLS-2$
			WebTarget postTarget = client.target(getRestUrl(null) + "/nosql/create"); //$NON-NLS-1$
			Response response = postTarget.request()
				.accept(MediaType.TEXT_HTML_TYPE) // Ensure that it routes to MVC
				.post(Entity.form(payload));
			assertEquals(303, response.getStatus());
		}
		
		// There should be at least one now
		{
			Response response = target.request().get();
			
			String json = response.readEntity(String.class);
			Map<String, Object> jsonObject = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, json);
			
			List<Map<String, Object>> byQueryLastName = (List<Map<String, Object>>)jsonObject.get("byQueryLastName"); //$NON-NLS-1$
			assertFalse(byQueryLastName.isEmpty());
			Map<String, Object> entry = byQueryLastName.get(0);
			assertEquals("CreatedUnitTest", entry.get("lastName")); //$NON-NLS-1$ //$NON-NLS-2$
			{
				Object customProp = entry.get("customProperty"); //$NON-NLS-1$
				assertTrue(customProp instanceof Map, "customProperty should be a Map"); //$NON-NLS-1$
				String val = (String)((Map<String, Object>)customProp).get("value"); //$NON-NLS-1$
				assertEquals("i am custom property", val); //$NON-NLS-1$
			}
			assertFalse(((String)entry.get("unid")).isEmpty()); //$NON-NLS-1$
		}
	}
	
	/**
	 * Tests to make sure a missing firstName is caught, which is enforced at the JAX-RS level.
	 */
	@Test
	public void testMissingFirstName() throws JsonException {
		Client client = getAnonymousClient();
		
		MultivaluedMap<String, String> payload = new MultivaluedHashMap<>();
		payload.putSingle("lastName", "CreatedUnitTest"); //$NON-NLS-1$ //$NON-NLS-2$
		payload.putSingle("customProperty", "i am custom property"); //$NON-NLS-1$ //$NON-NLS-2$
		WebTarget postTarget = client.target(getRestUrl(null) + "/nosql/create"); //$NON-NLS-1$
		Response response = postTarget.request().post(Entity.form(payload));
		assertEquals(400, response.getStatus());
	}
	
	/**
	 * Tests to make sure a missing lastName is caught, which is enforced at the JNoSQL level.
	 */
	@Test
	public void testMissingLastName() throws JsonException {
		Client client = getAnonymousClient();
		
		MultivaluedMap<String, String> payload = new MultivaluedHashMap<>();
		payload.putSingle("firstName", "CreatedUnitTest"); //$NON-NLS-1$ //$NON-NLS-2$
		payload.putSingle("customProperty", "i am custom property"); //$NON-NLS-1$ //$NON-NLS-2$
		WebTarget postTarget = client.target(getRestUrl(null) + "/nosql/create"); //$NON-NLS-1$
		Response response = postTarget.request().post(Entity.form(payload));
		// NB: this currently throws a 500 due to the exception being UndeclaredThrowableException (Issue #211)
		assertTrue(response.getStatus() >= 400, () -> "Response code should be an error; got " + response.getStatus()); //$NON-NLS-1$
	}
	
	@SuppressWarnings("unchecked")
	@Test
	@Disabled("QRP#executeToView is currently broken on Linux (12.0.1IF2)")
	public void testNoSqlNames() throws JsonException {
		Client client = getAnonymousClient();
		WebTarget target = client.target(getRestUrl(null) + "/nosql/servers"); //$NON-NLS-1$
		
		Response response = target.request().get();
		
		String json = response.readEntity(String.class);
		Map<String, Object> jsonObject = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, json);
		
		List<Map<String, Object>> all = (List<Map<String, Object>>)jsonObject.get("all"); //$NON-NLS-1$
		assertNotNull(all, () -> json);
		assertFalse(all.isEmpty(), () -> json);
		Map<String, Object> entry = all.get(0);
		assertEquals("CN=JakartaEE/O=OpenNTFTest", entry.get("serverName"), () -> json); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(((String)entry.get("unid")).isEmpty(), () -> json); //$NON-NLS-1$
		assertEquals(1d, ((Number)jsonObject.get("totalCount")).doubleValue(), () -> json); //$NON-NLS-1$
	}
	
	@SuppressWarnings({ "nls", "unchecked" })
	@Test
	public void testMultipartCreate() throws JsonException {
		Client client = getAnonymousClient();
		String unid;
		String lastName = "Fooson" + System.nanoTime();
		{
			WebTarget postTarget = client.target(getRestUrl(null) + "/nosql/create"); //$NON-NLS-1$
			
			MultipartFormDataOutput payload = new MultipartFormDataOutput();
			payload.addFormData("firstName", "Foo", MediaType.TEXT_PLAIN_TYPE);
			payload.addFormData("lastName", lastName, MediaType.TEXT_PLAIN_TYPE);
			
			Response response = postTarget.request()
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(payload, MediaType.MULTIPART_FORM_DATA_TYPE));
			assertEquals(200, response.getStatus());

			String json = response.readEntity(String.class);
			Map<String, Object> jsonObject = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, json);
			unid = (String)jsonObject.get("unid");
			assertNotNull(unid);
			assertFalse(unid.isEmpty());
		}
		
		// Fetch the doc by UNID
		{
			WebTarget getTarget = client.target(getRestUrl(null) + "/nosql/" + unid);
			
			Response response = getTarget.request()
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.get();
			assertEquals(200, response.getStatus());

			String json = response.readEntity(String.class);
			Map<String, Object> jsonObject = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, json);
			String getUnid = (String)jsonObject.get("unid");
			assertEquals(unid, getUnid);
			assertEquals(lastName, jsonObject.get("lastName"));
			
			// Make sure it has sensible mdate and cdate values
			Instant.parse((String)jsonObject.get("created"));
			Instant.parse((String)jsonObject.get("modified"));
		}
	}
	
	@SuppressWarnings({ "nls", "unchecked" })
	@Test
	public void testAttachmentCreate() throws JsonException {
		Client client = getAnonymousClient();
		String unid;
		String lastName = "Fooson" + System.nanoTime();
		{
			WebTarget postTarget = client.target(getRestUrl(null) + "/nosql/create"); //$NON-NLS-1$
			
			MultipartFormDataOutput payload = new MultipartFormDataOutput();
			payload.addFormData("firstName", "Foo", MediaType.TEXT_PLAIN_TYPE);
			payload.addFormData("lastName", lastName, MediaType.TEXT_PLAIN_TYPE);
			payload.addFormData("attachment", "<p>I am foo HTML</p>", MediaType.TEXT_HTML_TYPE, "foo.html");
			
			Response response = postTarget.request()
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(payload, MediaType.MULTIPART_FORM_DATA_TYPE));
			assertEquals(200, response.getStatus());

			String json = response.readEntity(String.class);
			Map<String, Object> jsonObject = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, json);
			unid = (String)jsonObject.get("unid");
			assertNotNull(unid);
			assertFalse(unid.isEmpty());
		}
		
		// Fetch the doc by UNID
		{
			WebTarget getTarget = client.target(getRestUrl(null) + "/nosql/" + unid);
			
			Response response = getTarget.request()
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.get();
			assertEquals(200, response.getStatus());

			String json = response.readEntity(String.class);
			Map<String, Object> jsonObject = (Map<String, Object>)JsonParser.fromJson(JsonJavaFactory.instance, json);
			String getUnid = (String)jsonObject.get("unid");
			assertEquals(unid, getUnid);
			assertEquals(lastName, jsonObject.get("lastName"));
			
			List<Map<String, Object>> attachments = (List<Map<String, Object>>)jsonObject.get("attachments");
			assertNotNull(attachments);
			assertFalse(attachments.isEmpty());
			assertTrue(attachments.stream().anyMatch(att -> "foo.html".equals(att.get("name"))));
		}
		
		// Fetch the attachment
		{
			WebTarget getTarget = client.target(getRestUrl(null) + "/nosql/" + unid + "/attachment/foo.html");

			Response response = getTarget.request().get();
			assertEquals(200, response.getStatus());

			String html = response.readEntity(String.class);
			assertEquals("<p>I am foo HTML</p>", html);
		}
	}
}
