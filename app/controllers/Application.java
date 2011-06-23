package controllers;

import helpers.MisoApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import models.MisoCheckin;
import models.MisoEpisode;
import models.MisoSeries;
import models.User;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.google.gson.Gson;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;

@With(Secure.class)
public class Application extends Controller {
	public static OAuthService	service	= null;
	public static int			GET		= 1;
	public static int			POST	= 2;

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

	public static void showSeries(Long media_id,Boolean error) {
		User user = getUser();
		isUserInitalized(user);
		List<MisoCheckin> seriesEpisodes = MisoCheckin.findSeriesEpisodes(user, media_id);

		MisoSeries misoSeries = MisoSeries.getSeriesDetails(media_id, user);
		MisoCheckin misoCheckin = MisoCheckin.findSeriesEpisode(user, media_id);
		render(user, seriesEpisodes, misoSeries, misoCheckin, error);
	}

	public static void checkinNextEpisode(Long media_id) {
		User user = getUser();
		MisoCheckin misoCheckin = MisoCheckin.findSeriesEpisode(user, media_id);
		Long nextEpisode = misoCheckin.episode_num + 1;
		Long season = misoCheckin.episode_season_num;
		Logger.debug("Check in to %s (%s) - %s", misoCheckin.media_title, media_id, nextEpisode);

		MisoEpisode me = getEpisodeDetails(media_id, user, nextEpisode, season);
		Boolean error = false;
		
		if (me != null) {
			String checkinBody = getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/checkins.json?media_id=" + media_id + "&season_num=" + me.season_num + "&episode_num=" + me.episode_num, POST);
			if (checkinBody.contains("That check-in is either a duplicate or invalid")) {
				Logger.info("Check in failed: %s", checkinBody);
				error = true;
			}
		}

		redirect("/series/" + media_id + "/"+error+"/");
	}

	/**
	 * @param media_id
	 * @param user
	 * @param episode
	 * @param season
	 */
	public static MisoEpisode getEpisodeDetails(Long media_id, User user, Long episode, Long season) {

		MisoEpisode misoEpisode = MisoEpisode.findEpisode(media_id, episode, season);

		if (misoEpisode == null) {
			String episodeBody = getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/episodes/show.json?media_id=" + media_id + "&season_num=" + season + "&episode_num=" + episode, GET);

			if (episodeBody.contains("Episode not found")) {
				episodeBody = getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/episodes/show.json?media_id=" + media_id + "&season_num=" + (season + 1) + "&episode_num=" + 1, GET);
			}

			if (episodeBody.contains("Episode not found")) {
				return null;
			}

			episodeBody = episodeBody.substring(11, episodeBody.length() - 1);
			misoEpisode = new Gson().fromJson(episodeBody, MisoEpisode.class);
			misoEpisode.media_id = media_id;
			misoEpisode.save();
			return misoEpisode;

		} else {
			return misoEpisode;
		}

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

		if (type == POST) {
			request = new OAuthRequest(Verb.POST, url);
		} else {
			request = new OAuthRequest(Verb.GET, url);
		}

		Application.getConnector().signRequest(accessToken, request);
		Response response = request.send();
		return response.getBody();
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