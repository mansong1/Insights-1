/*******************************************************************************
 * Copyright 2017 Cognizant Technology Solutions
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.cognizant.devops.platformregressiontest.test.correlationbuilder;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.cognizant.devops.platformregressiontest.common.CommonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class SaveCorrelationAPITest extends CorrelationTestData {

	private static final Logger log = LogManager.getLogger(CorrelationTestData.class);

	@BeforeMethod
	public void onInit() throws InterruptedException, IOException {
		jSessionID = getJsessionId();
		xsrfToken = getXSRFToken(jSessionID);

	}

	@Test(priority = 1)
	public void getCorrelationJson() {

		RestAssured.baseURI = CommonUtils.getProperty("baseURI") + CommonUtils.getProperty("getCorrelation");

		RequestSpecification httpRequest = RestAssured.given();

		httpRequest.header("Authorization", authorization);
		httpRequest.header("Content-Type", "application/json");
		Response response = httpRequest.request(Method.GET, "/");

		String correlationJson = response.getBody().asString();

		int statusCode = response.getStatusCode();
		Assert.assertEquals(statusCode, 200);
		Assert.assertTrue(correlationJson.contains("status"), "success");

	}

	@Test(priority = 2, dataProvider = "correlationdataprovider")
	public void saveCorrelation(String destinationToolName, String destinationToolCategory, String destinationLabelName,
			String sourceToolName, String sourceToolCategory, String sourceLabelName, String relationName,
			String isSelfRelation) {

		RestAssured.baseURI = CommonUtils.getProperty("baseURI") + CommonUtils.getProperty("saveCorrelation");

		RequestSpecification httpRequest = RestAssured.given();

		httpRequest.header(new Header("XSRF-TOKEN", xsrfToken));
		httpRequest.cookies("JSESSIONID", jSessionID, "grafanaOrg", CommonUtils.getProperty("grafanaOrg"),
				"grafanaRole", CommonUtils.getProperty("grafanaRole"), "XSRF-TOKEN", xsrfToken);
		httpRequest.header("Authorization", authorization);

		// Request payload sending along with post request

		JsonObject requestParam = new JsonObject();
		JsonObject destination = new JsonObject();
		JsonObject source = new JsonObject();
		destination.addProperty("toolName", destinationToolName);
		destination.addProperty("toolCategory", destinationToolCategory);
		destination.addProperty("labelName", destinationLabelName);
		JsonArray destinationFields = new JsonArray();
		destinationFields.add("key");
		destination.add("fields", destinationFields);
		requestParam.add("destination", destination);
		source.addProperty("toolName", sourceToolName);
		source.addProperty("toolCategory", sourceToolCategory);
		source.addProperty("labelName", sourceLabelName);
		JsonArray sourceFields = new JsonArray();
		sourceFields.add("key");
		source.add("fields", sourceFields);
		requestParam.add("source", source);
		requestParam.addProperty("relationName", relationName);
		JsonArray relationship_properties = new JsonArray();
		relationship_properties.add("");
		requestParam.add("relationship_properties", relationship_properties);
		requestParam.addProperty("isSelfRelation", isSelfRelation);

		httpRequest.header("Content-Type", "application/json");
		httpRequest.body(requestParam);

		Response response = httpRequest.request(Method.POST, "/");

		String correlationResponse = response.getBody().asString();
		log.debug("SaveCorrelationResponse", correlationResponse);

		int statusCode = response.getStatusCode();
		Assert.assertTrue(correlationResponse.contains("status"), "success");
		Assert.assertEquals(correlationResponse.contains("success"), true);
		Assert.assertEquals(statusCode, 200);

	}

	@Test(priority = 3, dataProvider = "correlationdataprovider")
	public void saveCorrelationFail(String destinationToolName, String destinationToolCategory,
			String destinationLabelName, String sourceToolName, String sourceToolCategory, String sourceLabelName,
			String relationName, String isSelfRelation) {

		RestAssured.baseURI = CommonUtils.getProperty("baseURI") + CommonUtils.getProperty("saveCorrelation");

		RequestSpecification httpRequest = RestAssured.given();
		httpRequest.header(new Header("XSRF-TOKEN", xsrfToken));
		httpRequest.cookies("JSESSIONID", jSessionID, "grafanaOrg", CommonUtils.getProperty("grafanaOrg"),
				"grafanaRole", CommonUtils.getProperty("grafanaRole"), "XSRF-TOKEN", xsrfToken);

		JsonObject requestParam = new JsonObject();

		httpRequest.header("Content-Type", "application/json");
		httpRequest.body(requestParam);

		// Response Object
		Response responseAgent = httpRequest.request(Method.POST, "/");

		String responseCorrelation = responseAgent.getBody().asString();
		log.debug("FailureResponse" + responseCorrelation);

		// Statuscode Validation
		int FailureStatusCode = responseAgent.getStatusCode();
		Assert.assertEquals(FailureStatusCode, 400);
		Assert.assertTrue(responseCorrelation.contains("status"), "failure");
		Assert.assertEquals(responseCorrelation.contains("failure"), true);

	}
}
