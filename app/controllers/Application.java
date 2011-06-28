package controllers;

import helpers.miso.MisoApi;

import java.util.List;

import models.User;
import models.miso.MisoCheckin;

import org.junit.Before;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import play.Logger;
import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.Router;

public class Application extends Controller {

	public static String	TEMPLATEPATH	= "Application/";

	@Before
	static void checkMobile() {
		renderArgs.put("mobile", session.get("mobile"));
	}

	public static void index() {
		render();
	}

	public static void setMobileMode(String redirect) {
		Logger.debug("Switching to Mobile mode and redirect to: %s", redirect);
		session.put("mobile", true);
		redirect(redirect);
	}

	public static void setDesktopMode(String redirect) {
		Logger.debug("Switching to Desktop mode and redirect to: %s", redirect);
		session.put("mobile", false);
		redirect(redirect);
	}

	public static void desktop() {
		// TODO: Workaround
		session.put("mobile", true);
		User user = getUser();
		MisoCheckin misocheckin = MisoCheckin.findLast(user);
		List<MisoCheckin> misocheckinlastseries = MisoCheckin.findLastSeries(user, 4);
		List<MisoCheckin> misocheckinlastmovies = MisoCheckin.findLastMovies(user, 4);
		render(getTemplate("desktop.html"), user, misocheckin, misocheckinlastseries, misocheckinlastmovies);
	}

	public static void register(Boolean mobile) {
		System.out.println("registering!");
		authenticate();
		// if (isMobile(mobile)) {
		// redirect("/m/");
		// }
		// redirect("/u/");
	}

	public static void authenticate() {
		System.out.println("auth");
		Token requestToken = Application.getConnector().getRequestToken();
		String authUrl = Application.getConnector().getAuthorizationUrl(requestToken);

		String strRequestToken = requestToken.getToken();
		String strRequestTokenSecret = requestToken.getSecret();

		User user = getUser(strRequestToken, strRequestTokenSecret);
		if (user == null) {
			user = new User("tmp", "tmp", "tmp");
		}
		new User("temp mail", "temp pw", "temp name");
		if (user.requestToken == null || user.authUrl == null || !user.requestToken.equals(requestToken) || !user.authUrl.equals(authUrl)) {
			user.requestToken = strRequestToken;
			user.requestTokenSecret = requestToken.getSecret();
			user.authUrl = authUrl;
			user.save();
		}

		session.put("requestToken", strRequestToken);
		session.put("requestTokenSecret", requestToken.getSecret());

		redirect(authUrl);
	}

	public static void oauthCallback(String oauth_token, String oauth_verifier) {

		User user = getUser(session.get("requestToken"), session.get("requestTokenSecret"));

		Verifier verifier = new Verifier(oauth_verifier);
		Token requestToken = new Token(user.requestToken, user.requestTokenSecret);
		Token accessToken = Application.getConnector().getAccessToken(requestToken, verifier);

		if (user.oauth_token == null || user.oauth_verifier == null || !user.oauth_token.equals(oauth_token) || !user.oauth_verifier.equals(oauth_verifier)) {
			user.oauth_token = oauth_token;
			user.oauth_verifier = oauth_verifier;
			user.accessToken = accessToken.getToken();
			user.accessTokenSecret = accessToken.getSecret();
			user.save();
		}
		user = User.updateMisoUserDetails(user);

		if (!session.get("requestTokenSecret").equals(user.requestTokenSecret)) {
			redirect("/");
		}
		render("Application/form.html", user);
	}

	public static void setPasswordEmail(Long userID, @Required(message = "E-Mail is required") String email, @Required(message = "A password is required") String password) {
		User user = User.findById(userID);
		if (!session.get("requestTokenSecret").equals(user.requestTokenSecret)) {
			redirect("/");
		}
		if (validation.hasErrors()) {
			render("Application/form.html", user);
		}
		flash.success("Thanks for registering %s, now login with e-mail and password", user.username);
		user.password = password;
		user.email = email;
		user.save();
		redirect("/login");
	}

	private static User getUser(String requestToken, String requestTokenSecret) {
		return User.find("requestToken = ? and requestTokenSecret = ?", requestToken, requestTokenSecret).first();
	}

	public static OAuthService getConnector() {
		if (Series.service == null) {
			Series.service = new ServiceBuilder().provider(MisoApi.class).apiKey("RLSKKwv083Ucv3WRlfPU").apiSecret("TY6Y9T7sznuFFdGClYM4H6OvgpJlpt0Dz9HZ4Tv4").callback(Router.getFullUrl(request.controller + ".oauthCallback")).build();
		}
		return Series.service;
	}

	/**
	 * Build an Json Request and returns the Body
	 * 
	 * @param user
	 *            The Authenticated User
	 * @param url
	 *            Oauth url to get the Json Data
	 * @return String Jsonbody
	 */
	public static String getJsonBodyforUrl(User user, String url, int type) {
		Logger.debug("Fetching %s", url);
		Token accessToken = new Token(user.accessToken, user.accessTokenSecret);
		OAuthRequest request;

		if (type == Series.POST) {
			request = new OAuthRequest(Verb.POST, url);
		} else {
			request = new OAuthRequest(Verb.GET, url);
		}

		getConnector().signRequest(accessToken, request);
		Response response = request.send();
		return response.getBody();
	}

	/**
	 * 
	 */
	public static String getTemplate(String template) {
		String path = Application.TEMPLATEPATH;
		if (isMobile()) {
			path += "mobile/";
		}
		return path + template;
	}

	static boolean isMobile() {
		return session.get("mobile") != null;
	}

	static User getUser() {
		return User.find(Security.connected());
	}
}
