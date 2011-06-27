package controllers;

import models.User;

import org.scribe.model.Token;
import org.scribe.model.Verifier;

import play.cache.Cache;
import play.data.validation.Required;
import play.libs.Codec;
import play.mvc.Controller;

public class Start extends Controller {

	public static void index(Boolean mobile) {

		render(mobile);
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
		Token requestToken = Application.getConnector("start").getRequestToken();
		String authUrl = Application.getConnector("start").getAuthorizationUrl(requestToken);

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

	public static void oauthCallbackstart(String oauth_token, String oauth_verifier) {

		User user = getUser(session.get("requestToken"), session.get("requestTokenSecret"));

		Verifier verifier = new Verifier(oauth_verifier);
		Token requestToken = new Token(user.requestToken, user.requestTokenSecret);
		Token accessToken = Application.getConnector("start").getAccessToken(requestToken, verifier);

		if (user.oauth_token == null || user.oauth_verifier == null || !user.oauth_token.equals(oauth_token) || !user.oauth_verifier.equals(oauth_verifier)) {
			user.oauth_token = oauth_token;
			user.oauth_verifier = oauth_verifier;
			user.accessToken = accessToken.getToken();
			user.accessTokenSecret = accessToken.getSecret();
			user.save();
		}
		user = User.updateMisoUserDetails(user);
		String randomID = Codec.UUID();
		if (!session.get("requestTokenSecret").equals(user.requestTokenSecret)) {
			redirect("/");
		}
		render("Start/form.html", randomID, user);
	}

	public static void setPasswordEmail(Long userID, @Required(message = "E-Mail is required") String email, @Required(message = "A password is required") String password, String randomID) {
		User user = User.findById(userID);
		if (!session.get("requestTokenSecret").equals(user.requestTokenSecret)) {
			redirect("/");
		}
		if (validation.hasErrors()) {
			render("Start/form.html", user, randomID);
		}
		flash.success("Thanks for registering %s, now login with e-mail and password", user.username);
		user.password = password;
		user.email = email;
		user.save();
		Cache.delete(randomID);
		redirect("/login");
	}

	private static User getUser(String requestToken, String requestTokenSecret) {
		return User.find("requestToken = ? and requestTokenSecret = ?", requestToken, requestTokenSecret).first();
	}

	private static boolean isMobile(Boolean mobile) {
		if (mobile == null) {
			return false;
		}
		return mobile;
	}
}
