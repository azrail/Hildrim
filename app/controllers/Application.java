package controllers;

import java.sql.Timestamp;

import helpers.MisoApi;
import models.Miso;
import models.User;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;

import com.google.gson.Gson;

@With(Secure.class)
public class Application extends Controller {
	public static OAuthService	service	= null;

	@Before
	static void setConnectedUser() {
		if (Security.isConnected()) {
			User user = User.find("byEmail", Security.connected()).first();
			renderArgs.put("user", user.username);
		}
	}

	public static void index() {

		isUserInitalized();

		// render();
	}

	public static boolean isUserInitalized() {

		boolean initalized = false;

		User user = getUser();
		if (user.accessToken == null) {
			redirect("/auth");
		}

		if (user.lastupdate == null) {
			Token accessToken = new Token(user.accessToken, user.accessTokenSecret);
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://gomiso.com/api/oauth/v1/users/show.json");
			getConnector().signRequest(accessToken, request);
			Response response = request.send();

			String userdetails = response.getBody().substring(8);
			userdetails = userdetails.substring(0, userdetails.length() - 1);

			Miso m = new Gson().fromJson(userdetails, Miso.class);
			user.miso = m;
			user.miso.save();
			user.lastupdate = new Timestamp(System.currentTimeMillis());
			user.save();
		}

		System.out.println(user.lastupdate);

		return initalized;

	}

	public static void authenticate() {
		getConnector();

		Token requestToken = service.getRequestToken();
		String authUrl = service.getAuthorizationUrl(requestToken);

		User user = getUser();
		if (user.requestToken == null || user.authUrl == null || !user.requestToken.equals(requestToken) || !user.authUrl.equals(authUrl)) {
			user.requestToken = requestToken.getToken();
			user.requestTokenSecret = requestToken.getSecret();
			user.authUrl = authUrl;
			user.save();
		}
		redirect(authUrl);
	}

	public static void oauthCallback(String oauth_token, String oauth_verifier) {
		User user = getUser();
		Verifier verifier = new Verifier(oauth_verifier);
		Token requestToken = new Token(user.requestToken, user.requestTokenSecret);
		Token accessToken = service.getAccessToken(requestToken, verifier);

		if (user.oauth_token == null || user.oauth_verifier == null || !user.oauth_token.equals(oauth_token) || !user.oauth_verifier.equals(oauth_verifier)) {
			user.oauth_token = oauth_token;
			user.oauth_verifier = oauth_verifier;
			user.accessToken = accessToken.getToken();
			user.accessTokenSecret = accessToken.getSecret();
			user.save();
		}

		redirect("/");
	}

	public static OAuthService getConnector() {
		if (service == null) {
			service = new ServiceBuilder().provider(MisoApi.class).apiKey("RLSKKwv083Ucv3WRlfPU").apiSecret("TY6Y9T7sznuFFdGClYM4H6OvgpJlpt0Dz9HZ4Tv4").callback(Router.getFullUrl(request.controller + ".oauthCallback")).build();
		}
		return service;
	}

	private static User getUser() {
		return User.find(Security.connected());
	}

}