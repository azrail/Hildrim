package controllers;

import helpers.MisoApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import models.MisoCheckin;
import models.User;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;

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
		User user = getUser();
		isUserInitalized(user);

		TreeMap<String, MisoCheckin> lse = MisoCheckin.findBaseSeries(user);
		List<MisoCheckin> lastSeriesEpisodes = new ArrayList();
		for (Entry<String, MisoCheckin> mc : lse.entrySet()) {
			lastSeriesEpisodes.add(mc.getValue());
		}
		render(user, lastSeriesEpisodes);
	}

	public static void showSeries(Long media_id) {
		User user = getUser();
		isUserInitalized(user);

		List<MisoCheckin> seriesEpisodes = MisoCheckin.findSeriesEpisodes(user, media_id);
		render(user, seriesEpisodes);
	}

	public static boolean isUserInitalized(User user) {

		if (user.accessToken == null) {
			redirect("/auth");
		}
		User.updateMisoUserDetails(user);
		MisoCheckin.updateCheckins(user);
		return true;
	}

	public static void authenticate() {
		Token requestToken = getConnector().getRequestToken();
		String authUrl = getConnector().getAuthorizationUrl(requestToken);

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
		Token accessToken = getConnector().getAccessToken(requestToken, verifier);

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