package scrapers.usps

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebElement}
import play.api.Configuration
import play.api.inject.Injector

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDate}
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters._

@Singleton
class MailpieceScraper @Inject()(injector: Injector, config: Configuration) {
  private lazy val username = config.get[String](MailpieceScraper.UsernameConfigKey)
  private lazy val password = config.get[String](MailpieceScraper.PasswordConfigKey)

  def getMailpieces: Seq[MailpieceScraper.Mailpiece] = {
    implicit val driver: RemoteWebDriver = injector.instanceOf[RemoteWebDriver]
    try {
      MailpieceScraper.login(username, password)
      MailpieceScraper.getMailpiecesByCurrentDay
    } finally {
      driver.quit()
    }
  }
}

object MailpieceScraper {
  private val StartingUrl = "https://reg.usps.com/portal/login"
  private val UsernameConfigKey = "usps.username"
  private val PasswordConfigKey = "usps.password"
  private val SelectedDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy")

  case class Mailpiece(id: String, base64EncodedImage: String, deliveryDate: LocalDate)

  object Mailpiece {
    private val imageUrlToBase64Script: String =
      """
        |let [ imageUrl, callback ] = arguments;
        |let xhr = new XMLHttpRequest();
        |xhr.onload = () => {
        |  let reader = new FileReader();
        |  reader.onloadend = () => { callback(reader.result); };
        |  reader.readAsDataURL(xhr.response);
        |};
        |xhr.open('GET', imageUrl);
        |xhr.responseType = 'blob';
        |xhr.send();
        |""".stripMargin

    def fromElement(element: WebElement, deliveryDate: LocalDate)(implicit driver: RemoteWebDriver): Mailpiece = {
      val imgSrc = element.findElement(By.cssSelector("img.mailpieceIMG")).getAttribute("src")
      val queryParams = imgSrc.split('?').last.split('&').map(_.split('=')).collect { case Array(k, v) => (k, v) }.toMap
      val id = queryParams("id")
      val base64EncodedImage = driver.executeAsyncScript(imageUrlToBase64Script, imgSrc).asInstanceOf[String]
      Mailpiece(id, base64EncodedImage, deliveryDate)
    }
  }

  private def waits(implicit driver: RemoteWebDriver) =
    new WebDriverWait(driver, Duration.ofSeconds(10))

  private def login(username: String, password: String)(implicit driver: RemoteWebDriver): Unit = {
    driver.get(StartingUrl)

    val usernameField = driver.findElement(By.name("username"))
    val passwordField = driver.findElement(By.name("password"))
    val submitButton = driver.findElement(By.id("btn-submit"))

    waits.until(ExpectedConditions.elementToBeClickable(submitButton))

    // wiggle mouse over submit button
    new Actions(driver)
      .moveToElement(submitButton, -3, -3)
      .pause(Duration.ofMillis(100))
      .moveToElement(submitButton, -3, 3)
      .pause(Duration.ofMillis(100))
      .moveToElement(submitButton, 3, 3)
      .pause(Duration.ofMillis(100))
      .moveToElement(submitButton, 3, -3)
      .pause(Duration.ofMillis(100))
      .click()
      .perform()

    waits.until(ExpectedConditions.textToBePresentInElementLocated(By.id("error-username"), "A Username is a required entry."))

    usernameField.sendKeys(username)
    passwordField.sendKeys(password)
    submitButton.click()

    waits.until(ExpectedConditions.textToBePresentInElementLocated(By.className("welcometext"), "Welcome to Informed Delivery"))
  }

  private def getMailpiecesByCurrentDay(implicit driver: RemoteWebDriver): Seq[Mailpiece] = {
    val dates = driver.findElements(By.cssSelector("ul#cp_week > li")).asScala
    val selectedDateIndex = dates.indexWhere(_.getAttribute("class").split(' ').contains("active"))
    require(selectedDateIndex >= 0, "unable to locate active date")

    val selectedDateStr = dates(selectedDateIndex).getAttribute("id")
    val selectedDate = LocalDate.parse(selectedDateStr, SelectedDateFormat)
    val mailpieces = driver.findElements(By.className("mailpiece")).asScala.toSeq.map(Mailpiece.fromElement(_, selectedDate))

    if (selectedDateIndex >= dates.length - 1) {
      mailpieces
    } else {
      dates(selectedDateIndex + 1).click()
      mailpieces ++ getMailpiecesByCurrentDay
    }
  }
}
