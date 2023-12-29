package modules

import com.google.inject.{AbstractModule, Provider}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.remote.RemoteWebDriver
import play.api.{Configuration, Environment}

import java.net.URL

class SeleniumModule(env: Environment, config: Configuration) extends AbstractModule {
  private val ExecuteHeadlessConfigKey = "selenium.execute-headless"
  private val RemoteWebDriverUrlConfigKey = "selenium.remote-web-driver-url"

  private lazy val executeHeadless: Boolean = config.get[Boolean](ExecuteHeadlessConfigKey)
  private lazy val remoteWebDriverUrl: Option[URL] = config.getOptional[URL](RemoteWebDriverUrlConfigKey)

  override def configure(): Unit = {
    bind(classOf[RemoteWebDriver]).toProvider(RemoteWebDriverProvider)
  }

  private object RemoteWebDriverProvider extends Provider[RemoteWebDriver] {
    override def get(): RemoteWebDriver = {
      val options = new ChromeOptions
      if (executeHeadless) options.addArguments("--headless=new")
      remoteWebDriverUrl.map(new RemoteWebDriver(_, options)).getOrElse(new ChromeDriver(options))
    }
  }
}
