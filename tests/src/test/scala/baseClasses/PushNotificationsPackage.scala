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

import common.rest.WskRestOperations
import common._
import io.restassured.RestAssured
import io.restassured.config.SSLConfig
import org.scalatest.BeforeAndAfterAll
import spray.json._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class PushNotificationsPackage
    extends TestHelpers
    with WskTestHelpers
    with WskActorSystem
    with BeforeAndAfterAll {

  implicit val wskprops = WskProps()
  val wsk = new Wsk()
  val wskRest: common.rest.WskRestOperations = new WskRestOperations
  val allowedActionDuration = 120 seconds

  // statuses for deployWeb
  val successStatus =
    """"status": "success""""

  val deployTestRepo =
    "https://github.com/ibm-functions/package-push-notifications"
  val packageName = "push-notifications"
  val deployAction = "/whisk.system/deployWeb/wskdeploy"
  val deployActionURL =
    s"https://${wskprops.apihost}/api/v1/web${deployAction}.http"

  //set parameters for deploy tests
  val node8RuntimePath = "runtimes/nodejs"
  val nodejs8folder = "../runtimes/nodejs";
  val nodejs8kind = "nodejs:8"

  //action definitions
  val actionSendMessage = packageName + "/send-message"
  val actionWebhook = packageName + "/webhook"


  behavior of "Push Notificiations Package"

  def deployNodeJS8 = {
    makePostCallWithExpectedResult(
      JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(node8RuntimePath),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)
      ),
      successStatus,
      200
    );
  }

  def deleteNodeJS8 = {
    // delete unique asset names
    wsk.action.delete(actionSendMessage)
    wsk.action.delete(actionWebhook)
    wsk.pkg.delete(packageName)
  }

  private def makePostCallWithExpectedResult(params: JsObject,
                                             expectedResult: String,
                                             expectedCode: Int) = {
    val response = RestAssured
      .given()
      .contentType("application/json\r\n")
      .config(
        RestAssured
          .config()
          .sslConfig(new SSLConfig().relaxedHTTPSValidation()))
      .body(params.toString())
      .post(deployActionURL)
    assert(response.statusCode() == expectedCode)
    response.body.asString should include(expectedResult)
    response.body.asString.parseJson.asJsObject
      .getFields("activationId") should have length 1
  }
}
