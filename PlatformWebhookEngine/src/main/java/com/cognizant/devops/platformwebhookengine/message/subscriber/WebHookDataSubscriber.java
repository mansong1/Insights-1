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
package com.cognizant.devops.platformwebhookengine.message.subscriber;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cognizant.devops.platformcommons.constants.MessageConstants;
import com.cognizant.devops.platformcommons.dal.neo4j.GraphDBException;
import com.cognizant.devops.platformcommons.dal.neo4j.Neo4jDBHandler;
//import com.cognizant.devops.platformengine.message.core.EngineStatusLogger;
import com.cognizant.devops.platformwebhookengine.message.factory.EngineSubscriberResponseHandler;
import com.cognizant.devops.platformwebhookengine.modules.aggregator.WebhookMappingData;
import com.cognizant.devops.platformwebhookengine.parser.InsightsWebhookParserFactory;
import com.cognizant.devops.platformwebhookengine.parser.InsightsWebhookParserInterface;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Envelope;

public class WebHookDataSubscriber extends EngineSubscriberResponseHandler {

	private static Logger log = LogManager.getLogger(WebHookDataSubscriber.class.getName());
	String responseTemplate;
	String toolName;
	JsonElement responseTemplateJson;

	public WebHookDataSubscriber(String routingKey, String responseTemplate, String toolName) throws Exception {
		super(routingKey);

		this.responseTemplate = responseTemplate;
		this.toolName = toolName;

	}

	List<WebhookMappingData> webhookMappingList = new ArrayList<WebhookMappingData>(0);

	public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
			throws IOException {

		Neo4jDBHandler dbHandler = new Neo4jDBHandler();
		String query = "UNWIND {props} AS properties " + "CREATE (n:" + toolName.toUpperCase() + ") "
				+ "SET n = properties";
		String message = new String(body, MessageConstants.MESSAGE_ENCODING);
		InsightsWebhookParserInterface webHookParser = InsightsWebhookParserFactory.getParserInstance(toolName);
		List<JsonObject> toolData = webHookParser.parseToolData(responseTemplate, message);
		log.debug(toolData);
		// Insert into Neo4j
		try {
			JsonObject graphResponse = dbHandler.bulkCreateNodes(toolData, null, query);
		} catch (GraphDBException e) {
			log.error("Error ..." + e);
		}
	}

	// processJson(json);
	// processJson(responseTemplateJson);

	private void processJson(JsonElement jsonElement) {
		List<JsonElement> list = new ArrayList<JsonElement>();
		// {"results":[{"columns":["n"],"data":[{"row":[{"name":"Test
		// me"}]},{"row":[{"name":"Test me"}]}]}],"errors":[]}
		if (jsonElement.isJsonNull()) {
			log.debug("Null value " + jsonElement);
		} else if (jsonElement.isJsonArray()) {

			JsonArray jsonArray = jsonElement.getAsJsonArray();
			// log.error("Json Array found " + jsonArray.size());
			if (jsonArray.size() > 0) {
				for (JsonElement jsonArrayElement : jsonArray) {
					if (jsonArrayElement.isJsonObject()) {
						list.addAll(Arrays.asList(jsonArrayElement));
						processJson(jsonArrayElement);
					} else {
						list.addAll(Arrays.asList(jsonArrayElement));
						// log.debug("Elemnet.." + jsonArrayElement);
					}
				}
			} else {
				// log.debug("Null value" + jsonArray);
			}
		} else {
			log.error(jsonElement);
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			log.debug(jsonObject);
			if (!jsonObject.isJsonNull()) {

				for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {

					if (entry.getValue().isJsonNull()) {
						// log.error("Null Value Found" + entry.getKey());
					} else if (entry.getValue().isJsonArray()) {
						processJson(entry.getValue());
						// log.debug("returened from rabbit mq function");

					} else if (!entry.getValue().isJsonPrimitive()) {
						parseJsonPrimitive(entry);
						// log.debug("Comeback from Json Promitive");

					} else {

						// log.debug(" Key..." + entry.getKey() + " Value..." + entry.getValue());
					}

				}
			}
		}
		log.debug(list);
	}

	private void parseJsonPrimitive(Map.Entry<String, JsonElement> entry) {
		log.debug("Entry..." + entry);

		if (entry.getValue().isJsonArray()) {
			processJson(entry.getValue());
		} else if (!entry.getValue().isJsonNull()) {
			log.error("Entered the not null conditionss");
			JsonObject jsonObjectInternal = entry.getValue().getAsJsonObject();
			for (Map.Entry<String, JsonElement> entryAgain : jsonObjectInternal.entrySet()) {
				if (entry.getValue().isJsonArray()) {
					processJson(entry.getValue());
				} else if (!entryAgain.getValue().isJsonNull() && !entryAgain.getValue().isJsonPrimitive()) {
					parseJsonPrimitive(entryAgain);
				} else {
					log.debug("Key internal ..." + entryAgain.getKey() + " Value..." + entryAgain.getValue());
				}
			}
		}

		else {
			log.debug(" value of Key internal is null  ..." + entry.getKey());
		}
	}
}
