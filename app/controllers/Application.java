package controllers;

import helpers.MisoApi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.Query;

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

import play.Logger;
import play.db.DB;
import play.db.jpa.JPA;
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

	public static void index(Boolean mobile) {
		User user = getUser();
		isUserInitalized(user);

		TreeMap<String, MisoCheckin> lse = MisoCheckin.findBaseSeries(user);
		List<MisoCheckin> lastSeriesEpisodes = new ArrayList();
		for (Entry<String, MisoCheckin> mc : lse.entrySet()) {
			lastSeriesEpisodes.add(mc.getValue());
		}
		if (isMobile(mobile)) {
			render("Application/indexMobile.html", user, lastSeriesEpisodes);
		}
		render(user, lastSeriesEpisodes);
	}

	private static boolean isMobile(Boolean mobile) {
		if (mobile == null) {
			return false;
		}
		return mobile;
	}

	public static void showSeries(Long media_id, Boolean error, Boolean mobile) {
		User user = getUser();
		isUserInitalized(user);
		List<MisoCheckin> seriesEpisodes = MisoCheckin.findSeriesEpisodes(user, media_id);

		MisoSeries misoSeries = MisoSeries.getSeriesDetails(media_id, user);
		MisoCheckin misoCheckin = MisoCheckin.findSeriesEpisode(user, media_id);

		TreeMap<String, MisoCheckin> lse = MisoCheckin.findBaseSeries(user);
		List<MisoCheckin> lastSeriesEpisodes = new ArrayList();
		for (Entry<String, MisoCheckin> mc : lse.entrySet()) {
			lastSeriesEpisodes.add(mc.getValue());
		}

		TreeSet<MisoEpisode> episodes = new TreeSet<MisoEpisode>();
		episodes.addAll(misoSeries.episodes);

		HashMap<Long, List<MisoEpisode>> seasons = new HashMap<Long, List<MisoEpisode>>();

		String[] seasons1 = misoSeries.seasons;
		for (String strSeason : seasons1) {
			Long season = new Long(strSeason);
			seasons.put(season, MisoEpisode.findEpisodes(media_id, season));
		}

		if (isMobile(mobile)) {
			render("Application/showSeriesMobile.html", user, seriesEpisodes, misoSeries, misoCheckin, error, lastSeriesEpisodes, seasons);
		}
		render(user, seriesEpisodes, misoSeries, misoCheckin, error, lastSeriesEpisodes, seasons);
	}

	public static void checkinNextEpisode(Long media_id, Boolean mobile) {
		User user = getUser();
		MisoCheckin misoCheckin = MisoCheckin.findSeriesEpisode(user, media_id);
		Long nextEpisode = misoCheckin.episode_num + 1;
		Long season = misoCheckin.episode_season_num;
		Logger.debug("Check in to %s (%s) - %s", misoCheckin.media_title, media_id, nextEpisode);

		MisoEpisode me = MisoEpisode.getEpisodeDetails(media_id, user, nextEpisode, season);
		Boolean error = false;

		if (me != null) {
			String checkinBody = getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/checkins.json?media_id=" + media_id + "&season_num=" + me.season_num + "&episode_num=" + me.episode_num, POST);
			if (checkinBody.contains("That check-in is either a duplicate or invalid")) {
				Logger.info("Check in failed: %s", checkinBody);
				error = true;
			}

			if (me.checkins == null) {
				me.checkins = 1L;
			} else {
				me.checkins = me.checkins + 1L;
			}
			me.save();

		}
		if (isMobile(mobile)) {
			redirect("/series/" + media_id + "/" + error + "/" + mobile + "/");
		}
		redirect("/series/" + media_id + "/" + error + "/");
	}

	public static void checkinEpisode(Long media_id, Long season, Long episode, Boolean mobile) {
		User user = getUser();
		Logger.debug("Check in to %s S%sE%s", media_id, season, episode);
		Boolean error = false;

		String checkinBody = getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/checkins.json?media_id=" + media_id + "&season_num=" + season + "&episode_num=" + episode, POST);
		if (checkinBody.contains("That check-in is either a duplicate or invalid")) {
			Logger.info("Check in failed: %s", checkinBody);
			error = true;
		}

		MisoEpisode me = MisoEpisode.getEpisodeDetails(media_id, user, episode, season);
		if (me != null) {
			if (me.checkins == null) {
				me.checkins = 1L;
			} else {
				me.checkins = me.checkins + 1L;
			}
			me.save();
		}

		if (isMobile(mobile)) {
			redirect("/series/" + media_id + "/" + error + "/" + mobile + "/");
		}
		redirect("/series/" + media_id + "/" + error + "/");
	}

	public static void updateCheckinCount() {
		ResultSet results = DB.executeQuery("select count(*) as checkins, media_id, episode_num, episode_season_num from misocheckin where isMovie = 0 group by media_id, episode_num, episode_season_num");
		try {
			while (results.next()) {
				Long checkins = results.getLong("checkins");

				Long media_id = results.getLong("media_id");
				Long episode_num = results.getLong("episode_num");
				Long episode_season_num = results.getLong("episode_season_num");

				MisoEpisode misoEpisode = MisoEpisode.findEpisode(media_id, episode_num, episode_season_num);
				misoEpisode.checkins = checkins;
				misoEpisode.save();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void showEpisode(Long media_id, Long season, Long episode, Boolean mobile) {
		User user = getUser();
		isUserInitalized(user);

		TreeMap<String, MisoCheckin> lse = MisoCheckin.findBaseSeries(user);
		List<MisoCheckin> lastSeriesEpisodes = new ArrayList();
		for (Entry<String, MisoCheckin> mc : lse.entrySet()) {
			lastSeriesEpisodes.add(mc.getValue());
		}

		MisoEpisode misoEpisode = MisoEpisode.findEpisode(media_id, episode, season);
		MisoSeries misoSeries = MisoSeries.findSeriesData(media_id);
		MisoCheckin misoCheckin = MisoCheckin.findSeriesEpisode(user, media_id);

		if (isMobile(mobile)) {
			render("Application/showEpisodeMobile.html", user, misoEpisode, misoSeries, misoCheckin, lastSeriesEpisodes);
		}

		render(user, misoEpisode, misoSeries, misoCheckin, lastSeriesEpisodes);
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