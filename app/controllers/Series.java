package controllers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import models.User;
import models.miso.MisoCheckin;
import models.miso.MisoEpisode;
import models.miso.MisoSeries;

import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.Gson;

@With(Secure.class)
public class Series extends Controller {
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
	
	public static String TEMPLATEPATH = "Series/"; 
	
	@Before
	static void checkMobile() {
		renderArgs.put("mobile", session.get("mobile"));
	}
	
	public static void index() {
		User user = Application.getUser();
		isUserInitalized(user);

		TreeMap<String, MisoCheckin> lse = MisoCheckin.findBaseSeries(user);
		List<MisoCheckin> lastSeriesEpisodes = new ArrayList();
		for (Entry<String, MisoCheckin> mc : lse.entrySet()) {
			lastSeriesEpisodes.add(mc.getValue());
		}

		render(getTemplate("index.html"),user, lastSeriesEpisodes);
	}

	public static void updateSeries(Long media_id, Boolean mobile) {
		User user = Application.getUser();
		MisoSeries.checkSeriesUpdates(media_id, user);
		redirect("/series/" + media_id + "/" + false + "/" + mobile + "/ ");
	}

	public static void showSeries(Long media_id, Boolean error) {

		User user = Application.getUser();
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
		render(getTemplate("showSeries.html"), user, seriesEpisodes, misoSeries, misoCheckin, error, lastSeriesEpisodes, seasons);
	}

	public static void checkinNextEpisode(Long media_id) {
		User user = Application.getUser();
		MisoCheckin misoCheckin = MisoCheckin.findSeriesEpisode(user, media_id);
		Long nextEpisode = misoCheckin.episode_num + 1;
		Long season = misoCheckin.episode_season_num;
		Logger.debug("Check in to %s (%s) - %s", misoCheckin.media_title, media_id, nextEpisode);

		MisoEpisode me = MisoEpisode.getEpisodeDetails(media_id, user, nextEpisode, season);
		Boolean error = false;

		if (me != null) {
			String checkinBody = Application.getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/checkins.json?media_id=" + media_id + "&season_num=" + me.season_num + "&episode_num=" + me.episode_num, POST);
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
		redirect("/series/" + media_id);
	}

	public static void checkinEpisode(Long media_id, Long season, Long episode) {
		User user = Application.getUser();
		Logger.debug("Check in to %s S%sE%s", media_id, season, episode);

		String checkinBody = Application.getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/checkins.json?media_id=" + media_id + "&season_num=" + season + "&episode_num=" + episode, POST);
		if (checkinBody.contains("That check-in is either a duplicate or invalid")) {
			Logger.info("Check in failed: %s", checkinBody);
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
		redirect("/series/" + media_id);
	}
	
	public static void showEpisode(Long media_id, Long season, Long episode, Boolean mobile) {
		User user = Application.getUser();
		isUserInitalized(user);

		TreeMap<String, MisoCheckin> lse = MisoCheckin.findBaseSeries(user);
		List<MisoCheckin> lastSeriesEpisodes = new ArrayList();
		for (Entry<String, MisoCheckin> mc : lse.entrySet()) {
			lastSeriesEpisodes.add(mc.getValue());
		}

		MisoEpisode misoEpisode = MisoEpisode.findEpisode(media_id, episode, season);
		MisoSeries misoSeries = MisoSeries.findSeriesData(media_id);
		MisoCheckin misoCheckin = MisoCheckin.findSeriesEpisode(user, media_id);

		render(getTemplate("showEpisode.html"),user, misoEpisode, misoSeries, misoCheckin, lastSeriesEpisodes);

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
			String episodeBody = Application.getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/episodes/show.json?media_id=" + media_id + "&season_num=" + season + "&episode_num=" + episode, GET);

			if (episodeBody.contains("Episode not found")) {
				episodeBody = Application.getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/episodes/show.json?media_id=" + media_id + "&season_num=" + (season + 1) + "&episode_num=" + 1, GET);
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
		//user = User.updateMisoUserDetails(user);
		//MisoCheckin.updateCheckins(user);
		return true;
	}
	/**
	 * 
	 */
	public static String getTemplate(String template) {
		String path = Series.TEMPLATEPATH;
		if (Application.isMobile()) {
			path += "mobile/";
		}
		return path + template;
	}
}