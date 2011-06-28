package controllers;

import java.sql.ResultSet;
import java.sql.SQLException;

import models.User;
import models.miso.MisoCheckin;
import models.miso.MisoEpisode;

import org.junit.Before;

import play.Logger;
import play.db.DB;
import play.mvc.Controller;
import play.mvc.Router;

public class Service extends Controller {

	public static String	TEMPLATEPATH	= "Service/";

	@Before
	static void checkMobile() {
		renderArgs.put("mobile", session.get("mobile"));
	}

	/**
	 * 
	 */
	public static String getTemplate(String template) {
		String path = Service.TEMPLATEPATH;
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

	public static void updateMiso() {
		User user = getUser();
		User.updateMisoUserDetails(user);
		MisoCheckin.updateCheckins(user);
		Long cnt = Service.updateCheckinCount();
		Logger.debug("Updated %d Checkins", cnt);
		Logger.debug("Redirecting to %s", Router.getFullUrl("Application.desktop"));
		redirect("Application.desktop");
	}

	public static Long updateCheckinCount() {
		Logger.debug("Updating Checkins...");
		Long cnt = 1L;
		ResultSet results = DB.executeQuery("select count(*) as checkins, media_id, episode_num, episode_season_num from MisoCheckin where isMovie = 0 group by media_id, episode_num, episode_season_num");
		try {
			while (results.next()) {
				Long checkins = results.getLong("checkins");
				Long media_id = results.getLong("media_id");
				Long episode_num = results.getLong("episode_num");
				Long episode_season_num = results.getLong("episode_season_num");

				MisoEpisode misoEpisode = MisoEpisode.findEpisode(media_id, episode_num, episode_season_num);
				misoEpisode.checkins = checkins;
				misoEpisode.save();
				cnt++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return cnt;
	}
}
