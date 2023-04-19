/**
 * Copyright (c) 2018-2023 Contributors to the XPages Jakarta EE Support Project
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
package it.org.openntf.xsp.jakartaee.nsf.microprofile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import it.org.openntf.xsp.jakartaee.AbstractWebClientTest;
import it.org.openntf.xsp.jakartaee.docker.DominoContainer;

@SuppressWarnings("nls")
public class TestOpenAPI extends AbstractWebClientTest {
	@ParameterizedTest
	@ValueSource(strings = { "openapi", "openapi.yaml" })
	public void testOpenAPIYaml(String path) {
		Client client = getAnonymousClient();
		WebTarget target = client.target(getRestUrl(null) + "/" + path);
		Response response = target.request().get();
		
		String yaml = response.readEntity(String.class);
		assertTrue(yaml.startsWith("---\nopenapi: 3.0"), () -> yaml);
		assertTrue(yaml.contains("  /adminrole:"));
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "openapi", "openapi.json" })
	public void testOpenAPIJson(String path) {
		Client client = getAnonymousClient();
		WebTarget target = client.target(getRestUrl(null) + "/" + path);
		Response response = target.request()
			.accept(MediaType.APPLICATION_JSON_TYPE)
			.get();
		
		String json = response.readEntity(String.class);
		JsonObject obj = Json.createReader(new StringReader(json)).readObject();
		
		// Check for a known resource
		JsonObject paths = obj.getJsonObject("paths");
		assertTrue(paths.containsKey("/adminrole"));

		JsonObject info = obj.getJsonObject("info");
		assertEquals("XPages JEE Example", info.getString("title"));
		
		// Check for the presence of a version from $TemplateBuild
		String mavenVersion = DominoContainer.getMavenVersion();
		if(mavenVersion.endsWith("-SNAPSHOT")) {
			mavenVersion = mavenVersion.substring(0, mavenVersion.length()-"-SNAPSHOT".length());
		}
		if(!info.containsKey("version")) {
			fail("Encountered unexpected JSON: " + json);
		}
		String version = info.getString("version");
		assertTrue(version.startsWith(mavenVersion), "Expected version '" + version + "' to start with '" + mavenVersion + "'");
		
		JsonArray servers = obj.getJsonArray("servers");
		JsonObject server0 = servers.getJsonObject(0);
		assertEquals(getRestUrl(null), server0.getString("url"));
	}
	
	@ParameterizedTest
	@ValueSource(strings = { "openapi", "openapi.json" })
	public void testOpenAPIBundleDb(String path) {
		Client client = getAnonymousClient();
		WebTarget target = client.target(getBundleNsfRestUrl(null) + "/" + path);
		Response response = target.request()
			.accept(MediaType.APPLICATION_JSON_TYPE)
			.get();
		
		String json = response.readEntity(String.class);
		JsonObject obj = Json.createReader(new StringReader(json)).readObject();
		
		// Check for the presence overridden versions
		JsonObject info = obj.getJsonObject("info");
		try {
			assertEquals("OpenAPI Overridden Title", info.getString("title"));
			assertEquals("3.1.1.override", info.getString("version"));
			JsonObject license = info.getJsonObject("license");
			assertEquals("http://some.license/url", license.getString("url"));
			
			JsonArray servers = obj.getJsonArray("servers");
			JsonObject server0 = servers.getJsonObject(0);
			assertEquals("http://override.server/path", server0.getString("url"));
		} catch(NullPointerException e) {
			fail("Encountered NPE with JSON " + json, e);
		}
	}
}
