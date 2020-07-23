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
package com.cognizant.devops.platformregressiontest.test.groupsanduser;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.cognizant.devops.platformregressiontest.common.CommonUtils;
import com.google.gson.JsonObject;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class SwitchUserOrgsAPITest extends GroupsAndUserTestData {

	private static final Logger log = LogManager.getLogger(SwitchUserOrgsAPITest.class);

	@BeforeMethod
	public void onInit() throws InterruptedException, IOException {
		jSessionID = getJsessionId();
		xsrfToken = getXSRFToken(jSessionID);
	}

	@Test(priority = 1)
	public void switchUserOrgs() {

		RestAssured.baseURI = CommonUtils.getProperty("baseURI") + CommonUtils.getProperty("switchUser");
		RequestSpecification httpRequest = RestAssured.given();

		httpRequest.header(new Header("XSRF-TOKEN", xsrfToken));
		httpRequest.cookies("JSESSIONID", jSessionID, "grafanaOrg", CommonUtils.getProperty("grafanaOrg"),
				"grafanaRole", CommonUtils.getProperty("grafanaRole"), "XSRF-TOKEN", xsrfToken);
		httpRequest.header("Authorization", authorization);
		httpRequest.queryParams("orgId", CommonUtils.getProperty("orgId2"));

		// Request payload sending along with post request

		JsonObject requestParam = new JsonObject();

		httpRequest.header("Content-Type", "application/json");
		httpRequest.body(requestParam);

		Response response = httpRequest.request(Method.POST, "/");

		String switchUserResponse = response.getBody().asString();

		log.debug("switchUserResponse" + switchUserResponse);

		int statusCode = response.getStatusCode();
		Assert.assertEquals(statusCode, 200);
		Assert.assertEquals(switchUserResponse.contains("success"), true);
		Assert.assertTrue(switchUserResponse.contains("data"), "Active organization changed");

	}

	@Test(priority = 2)
	public void switchUserOrgToAnotherOrg() {

		RestAssured.baseURI = CommonUtils.getProperty("baseURI")
				+ "/PlatformService/accessGrpMgmt/switchUserOrg?orgId=1";
		RequestSpecification httpRequest = RestAssured.given();

		httpRequest.header(new Header("XSRF-TOKEN", xsrfToken));
		httpRequest.cookies("JSESSIONID", jSessionID, "grafanaOrg", CommonUtils.getProperty("grafanaOrg"),
				"grafanaRole", CommonUtils.getProperty("grafanaRole"), "XSRF-TOKEN", xsrfToken);
		httpRequest.header("Authorization", authorization);
		httpRequest.queryParams("orgId", CommonUtils.getProperty("orgId1"));

		// Request payload sending along with post request

		httpRequest.header("Content-Type", "application/json");
		Response response = httpRequest.request(Method.POST, "/");

		String switchUserOrgResponse = response.getBody().asString();

		log.debug("switchUserResponse" + switchUserOrgResponse);

		int statusCode = response.getStatusCode();
		Assert.assertEquals(statusCode, 200);
		Assert.assertEquals(switchUserOrgResponse.contains("success"), true);
		Assert.assertTrue(switchUserOrgResponse.contains("data"), "Active organization changed");

	}

}
