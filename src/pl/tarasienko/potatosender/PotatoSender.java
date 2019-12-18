package pl.tarasienko.potatosender;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;

public class PotatoSender
{
	private int min = 0, max = 0;
	private String message = "", login = "", password = "";
	private String[] linksToImages = new String[0];
	private WebDriver driver;
	private Thread loginThread, sendingThread;
	private JLabel jLabel_Status;
	private JButton jButton_Send;
	
	
	public PotatoSender(JLabel jLabel_Status, JButton jButton_Send)
	{
		this.jLabel_Status = jLabel_Status;
		this.jButton_Send = jButton_Send;
	}

	public boolean isWorking()
	{
		if(driver != null && (loginThread != null || sendingThread != null))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public void setData(String login, String password, int min, int max, String message, String[] linksToImages)
	{
		this.login = login;
		this.password = password;
		this.min = min;
		this.max = max;
		this.message = message;
		this.linksToImages = linksToImages;
	}

	public void openBrowser()
	{
		if(driver == null) // Open browser if it isn't already open.
		{
			new Thread()
			{
				@Override
				public void run()
				{
					try
					{
						updateUI("Opening browser...", "...", false);
						DesiredCapabilities dc = new DesiredCapabilities();
						dc.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.IGNORE);
						driver = new ChromeDriver(dc);
						startSendingPotatoes();
					}
					catch(Exception e)
					{
						driver = null;
						updateUI("Offline", "Send", true);
					}
				}
			}.start();
		}
		else
		{
			startSendingPotatoes();
		}
	}

	private void startSendingPotatoes()
	{
		if(!isLoggedIn())
		{
			logIn();
		}
		else
		{
			sendMessage();
		}
	}

	private boolean isLoggedIn()
	{
		return (driver.findElements(By.className("logged-user")).size() > 0); // New class in DOM appears when we are logged in.
	}

	private void logIn()
	{
		loginThread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					updateUI("Logging in progress...", "Cancel", true);
					driver.get("https://www.wykop.pl/login/");
					WebElement inputTextLogin = driver.findElements(By.name("user[username]")).get(1);
					WebElement inputTextPassword = driver.findElements(By.name("user[password]")).get(2);

					inputTextLogin.sendKeys(login);
					inputTextPassword.sendKeys(password);
					//inputTextPassword.sendKeys(Keys.TAB + Keys.SPACE.toString()); // For recaptcha.
					//Thread.sleep(5000); // For recaptcha.

					if(loginThread == null) // If is null it means client pressed the "Cancel" button so don't log in.
					{
						return;
					}
					updateUI("Logging in progress...", "...", false);
					inputTextPassword.submit();

					if(isLoggedIn())
					{
						updateUI("Logged in", "...", false);
						loginThread = null;
						sendMessage();
					}
					else // Log in failed.
					{
						loginThread = null;
						updateUI("Logging failed", "Log in", true);
					}
				}
				catch(Exception e)
				{
					loginThread = null;
					updateUI("Logging failed", "Log in", true);
				}
			}
		};
		loginThread.start();
	}

	private void sendMessage()
	{
		sendingThread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					updateUI("Sending potato...", "Cancel", true);
					String url = "https://www.wykop.pl/tag/charytatywnapyra/";

					Random rand = new Random();
					while(true)
					{
						if(!driver.getCurrentUrl().equals(url))
						{
							driver.get(url); // Open webpage where we will send messages.
						}

						//<editor-fold desc="Click on the textarea:">
						WebElement textareaMessage = driver.findElement(By.tagName("textarea"));
						Actions actions = new Actions(driver);
						actions.moveToElement(textareaMessage).click().build().perform();
						textareaMessage.clear();
						textareaMessage.sendKeys(message);
						//</editor-fold>

						//<editor-fold desc="Click on the button with camera icon:">
						WebElement cameraButton = driver.findElement(By.className("openAddMediaOverlay"));
						cameraButton.sendKeys(Keys.ENTER);
						//</editor-fold>

						//<editor-fold desc="Paste link to potato image & send it on the server & send message:">
						WebElement inputLinkOfImage = driver.findElement(By.className("embedUrl"));
						if(linksToImages.length > 0)
						{
							inputLinkOfImage.sendKeys(linksToImages[rand.nextInt(linksToImages.length)]);
							inputLinkOfImage.submit();

							while(driver.findElements(By.className("embedUrl")).size() > 0)
							{
								Thread.sleep(1000); // Wait 1 second if the image is still sending to the server.
							}

							//<editor-fold desc="Send potato message:">
							if(sendingThread == null) // If is null it means client pressed the "Cancel" button so don't send message.
							{
								return;
							}
							WebElement sendMessageButton = driver.findElement(By.className("gaSubmit_tag"));
							sendMessageButton.sendKeys(Keys.ENTER);
							//</editor-fold>
						}
						//</editor-fold>

						//<editor-fold desc="Wait for the next message to be sent:">
						int sleepTime = rand.nextInt(max - min) + min;
						int napTime; // This var is for update the status label with new time.
						while(sleepTime > 0)
						{
							if(sleepTime >= 5)
							{
								napTime = 5; // Refresh time for every 5 seconds.
							}
							else
							{
								napTime = sleepTime;
							}
							jLabel_Status.setText("Time to send next potato: " + sleepTime + "s");

							Thread.sleep(napTime * 1000);
							sleepTime -= napTime;
						}
						jLabel_Status.setText("Sending potato...");
						//</editor-fold>
					}
				}
				catch(UnhandledAlertException e) // Sometimes can pop up server overload alert.
				{
					try
					{
						updateUI("120s break...", "Cancel", true);
						Alert alert = driver.switchTo().alert();
						alert.accept();

						Thread.sleep(120000); // Wait 120 seconds. If we will spam for example every 5 seconds the alarm won't disappear.
						sendMessage();
					}
					catch(Exception e2)
					{
						sendingThread = null;
						updateUI("Potatoes sending is stopped", "Send potatoes", true);
					}
				}
				catch(Exception e)
				{
					sendingThread = null;
					updateUI("Potatoes sending is stopped", "Send potatoes", true);
				}
			}
		};
		sendingThread.start();
	}

	public void stopSendingPotatoes()
	{
		if(sendingThread != null) // If we sending potatoes then stop it and don't log out.
		{
			sendingThread.interrupt();
			sendingThread = null;
			updateUI("Potatoes sending is stopped", "Send potatoes", true);
		}
		else if(loginThread != null) // If this thread exists then we are in login process.
		{
			loginThread.interrupt();
			loginThread = null;
			updateUI("Login process is canceled", "Log in", true);
		}
	}

	public void logOut()
	{
		List<WebElement> buttonsLogoutList = driver.findElements(By.className("fa-power-off"));
		if(buttonsLogoutList.size() > 1)
		{
			buttonsLogoutList.get(1).click();
		}
	}

	private void updateUI(String labelText, String buttonText, boolean buttonEnabled)
	{
		jLabel_Status.setText(labelText);
		jButton_Send.setText(buttonText);
		jButton_Send.setEnabled(buttonEnabled);
	}
}
