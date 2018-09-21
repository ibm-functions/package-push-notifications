/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package packages

import common.{TestHelpers, WskTestHelpers, _}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import spray.json.DefaultJsonProtocol._
import spray.json._

@RunWith(classOf[JUnitRunner])
class CredentialsCFPushTests
    extends PushNotificationsPackage
    with TestHelpers
    with WskTestHelpers{
  val credentials = TestUtils.getVCAPcredentials("imfpush")
  val appSecret = JsString(credentials.get("appSecret"));
  val credentialsUrl = credentials.get("url");
  val adminURL = credentials.get("admin_url");
  val apiHost = adminURL.split("/")(2);
  val appGuid = JsString(credentialsUrl.split("/").last);

  // action names
  val sendMessageName = "push-notifications/send-message"
  val webhookName = "push-notifications/webhook"

  // send-message variables
  val messageUrl = JsString("www.google.com");
  val messageText = JsString("""This is pushnotifications Testing""");
  val unicodeMessage = JsString("""\ue04a""");
  val accentMessage = JsString("""Máxima de 33 C and Mínima de 26 C""");

  // webhook variables
  val myTriggerFQN = JsString("""/guest/myTrigger""");
  val myTriggerName = "myTrigger"
  val events = JsString("""onDeviceRegister""");
  val lifecycleEventDelete = JsString("""DELETE""");

  behavior of "Push Package with CF style Credentials"

    it should "Deploy nodejs8 push-notificiations package and actions" in {
      deployNodeJS8
    }

    it should "Send Notification action" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> messageText))){
        _.response.result.get.toString should include (messageText.convertTo[String])
      }
    }

    it should "Send Notification action with unicode message" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> unicodeMessage))){
        _.response.result.get.toString should include (unicodeMessage.convertTo[String])
      }
    }

    it should "Send Notification action with accent message" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> accentMessage))){
        _.response.result.get.toString should include (accentMessage.convertTo[String])
      }
    }

    it should "Send Notification action with messageUrl" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> messageText, "messageUrl"-> messageUrl))){
        _.response.result.get.toString should include (messageText.convertTo[String])
      }
    }

    it should "Send Notification action using admin_url" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> messageText, "admin_url"-> adminURL.toJson))){
        _.response.result.get.toString should include (messageText.convertTo[String])
      }
    }

    it should "Send Notification action using bad admin_url" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> messageText, "admin_url"-> "//mobile.bad.host/pathname".toJson))){
        _.response.success shouldBe false
      }
    }

    it should "Send Notification action using apiHost" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> messageText, "apiHost"-> apiHost.toJson))){
        _.response.result.get.toString should include (messageText.convertTo[String])
      }
    }

    it should "Send Notification action using bad apiHost" in {
      withActivation(wsk.activation,wsk.action.invoke(sendMessageName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "messageText" -> messageText, "apiHost"-> "mobile.bad.host".toJson))){
        _.response.success shouldBe false
      }
    }

    it should "Create a webhook with trigger name" in {
      withActivation(wsk.activation,wsk.action.invoke(webhookName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "triggerName" -> myTriggerFQN, "events" -> events))){
        _.response.result.get.toString should include (myTriggerName)
      }
    }

    it should "Fail to create a webhook with a trigger name that already exists" in {
      withActivation(wsk.activation,wsk.action.invoke(webhookName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "triggerName" -> myTriggerFQN, "events" -> events))){
        _.response.result.get.toString should include ("""The resource 'Webhook' already exists on the server""")
      }
    }

    it should "Delete the webhook with trigger name" in {
      withActivation(wsk.activation,wsk.action.invoke(webhookName, Map("appSecret" -> appSecret, "appGuid" -> appGuid, "triggerName" -> myTriggerFQN, "events" -> events, "lifecycleEvent" -> lifecycleEventDelete))){
        _.response.result.get.toString should include ("""{"response":""}""")
      }
    }

    it should "Delete nodejs8 push-notifications package and actions" in {
      deleteNodeJS8
    }
}
