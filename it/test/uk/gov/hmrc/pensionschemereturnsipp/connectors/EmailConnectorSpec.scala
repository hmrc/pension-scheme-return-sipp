package uk.gov.hmrc.pensionschemereturnsipp.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.pensionschemereturnsipp.models.PensionSchemeId.PsaId
import uk.gov.hmrc.pensionschemereturnsipp.utils.WireMockHelper

class EmailConnectorSpec extends AsyncWordSpec with Matchers with WireMockHelper with EitherValues {

  override protected def portConfigKey: String = "microservice.services.email.port"

  private val url: String = "/hmrc/email"
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val testPsaId = PsaId("A1234567")
  private val testPstr = "87219363YN"
  private val requestId = "test-Request-Id"
  private val testEmailAddress = "test@test.com"
  private val testTemplate = "testTemplate"

  private lazy val connector = injector.instanceOf[EmailConnector]

  "Email Connector" must {
    "return an EmailSent" when {
      "email sent successfully with status 202 (Accepted)" in {
        server.stubFor(
          post(urlEqualTo(url)).willReturn(
            aResponse()
              .withStatus(Status.ACCEPTED)
              .withHeader("Content-Type", "application/json")
          )
        )
        connector
          .sendEmail(
            testPsaId,
            requestId,
            testPstr,
            testEmailAddress,
            testTemplate,
            Map.empty
          )
          .map { result =>
            result.value mustBe ()
          }
      }
    }

    "return an EmailNotSent" when {
      "email service is down" in {
        server.stubFor(
          post(urlEqualTo(url)).willReturn(
            serviceUnavailable()
              .withHeader("Content-Type", "application/json")
          )
        )

        connector
          .sendEmail(
            testPsaId,
            requestId,
            testPstr,
            testEmailAddress,
            testTemplate,
            Map.empty
          )
          .map { result =>
            result.left.value mustBe "Failed to send email 503"
          }
      }

      "email service returns back with 204 (No Content)" in {
        server.stubFor(
          post(urlEqualTo(url)).willReturn(
            noContent()
              .withHeader("Content-Type", "application/json")
          )
        )
        connector
          .sendEmail(
            testPsaId,
            requestId,
            testPstr,
            testEmailAddress,
            testTemplate,
            Map.empty
          )
          .map { result =>
            result.left.value mustBe "Failed to send email 204"
          }
      }
    }
  }
}
