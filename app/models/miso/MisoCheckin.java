package models.miso;

import helpers.miso.MisoCheckinPojo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.TreeMap;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import models.User;

import org.apache.commons.collections.list.TreeList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.Logger;
import play.data.validation.MaxSize;
import play.db.DB;
import play.db.jpa.Model;
import play.libs.F.Promise;

import com.google.gson.Gson;

import controllers.Application;
import controllers.Service;

/**
 * Represents an Miso Checkin
 * 
 * @author prime
 * 
 */
@Entity
public class MisoCheckin extends Model {

	/**
	 * Base Checkin URL
	 */
	private static String	CHECKINURL	= "http://gomiso.com/api/oauth/v1/checkins.json?user_id=";

	/**
	 * The Authenticated User
	 */
	@ManyToOne
	public User				user;

	/**
	 * Serie oder Film
	 */
	public Boolean			isMovie;
	/**
	 * The id for this checkin object. 9876
	 */
	public Long				checkin_id;
	/**
	 * The timestamp for when this checkin was created
	 * "2010-12-11T00:21:44.000Z"
	 */
	public Timestamp		created_at;
	/**
	 * The comment attached for this checkin. Dexter is a great show
	 */
	public String			comment;
	/**
	 * The user username for this checkin. john24
	 */
	public String			user_username;
	/**
	 * The user name for this checkin. John Smith
	 */
	public String			user_full_name;
	/**
	 * The profile image for the given user.
	 * http://gomiso.com/uploads/BAhbCFsHOgZm.png
	 */
	@Lob
	@MaxSize(10000)
	public String			user_profile_image_url;
	/**
	 * The media id for this checkin. 5678
	 */
	public Long				media_id;
	/**
	 * The media title for this checkin. The Dark Knight
	 */
	public String			media_title;
	/**
	 * The poster image for the given media.
	 * http://gomiso.com/uploads/BAhbCFsHOgZm.png
	 */
	@Lob
	@MaxSize(10000)
	public String			media_poster_url;
	/**
	 * The thumbnail poster image for the given media.
	 * http://gomiso.com/uploads/BAhbCFsHOgZm.png
	 */
	@Lob
	@MaxSize(10000)
	public String			media_poster_url_small;
	/**
	 * The number of the episode checked into (if this is an episode checkin) 12
	 */
	public Long				episode_num;
	/**
	 * The number of the season the episode belonged to (if this is an episode
	 * checkin) 12
	 */
	public Long				episode_season_num;
	/**
	 * The label of the episode checked into (if this is an episode checkin)
	 * S03E12
	 */
	public String			episode_label;
	/**
	 * The title of the episode checked into (if this is an episode checkin) It
	 * Takes Two to Tango
	 */
	public String			episode_title;
	/**
	 * The image url of the episode checked into (if this is an episode checkin)
	 * http://gomiso.com/abcdefg.jpg
	 */
	@Lob
	@MaxSize(10000)
	public String			episode_poster_url;
	/**
	 * The thumbnail image url of the episode checked into (if this is an
	 * episode checkin) http://gomiso.com/abcdefg.jpg
	 */
	@Lob
	@MaxSize(10000)
	public String			episode_poster_url_small;

	public MisoMedia getMisoMedia() {
		return MisoMedia.getMediaDetails(media_id, user);
	}
	
	public MisoEpisode getMisoEpisode() {
		return MisoEpisode.findEpisode(media_id, episode_num, episode_season_num);
	}

	/**
	 * Checks Miso Checkins
	 * 
	 * @param user
	 *            The Authenticated User
	 */
	public static void updateCheckins(User user) {

		if (!hasCheckins()) {
			String url = CHECKINURL + user.miso.id.toString() + "&count=20";
			getCheckinBody(user, url);

			while (getOlderCheckins(user)) {
				Logger.debug("Getting More Checkins...");
			}
		}

		while (getNewestCheckins(user)) {
			Logger.debug("Getting More Checkins...");
		}
		
	}

	/**
	 * Checks recursive for newer Checkins
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return true or false
	 */
	private static boolean getNewestCheckins(User user) {

		Long checkinid = findLast(user).checkin_id;

		String url = CHECKINURL + user.miso.id.toString() + "&since_id=" + checkinid + "&count=20";
		String[] checkins = getCheckinBody(user, url);

		if (checkins.length == 1) {
			return false;
		}

		return true;
	}

	/**
	 * Checks recursive for older Checkins
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return true or false
	 */
	private static boolean getOlderCheckins(User user) {

		Long checkinid = findFirst(user).checkin_id;
		String url = CHECKINURL + user.miso.id.toString() + "&max_id=" + checkinid + "&count=20";
		String[] checkins = getCheckinBody(user, url);

		if (checkins.length == 1) {
			return false;
		}

		return true;
	}

	/**
	 * Build the Oauth Request and fetch the Checkins
	 * 
	 * @param user
	 *            The Authenticated User
	 * @param url
	 *            Checkin url
	 * @return String[] json Checkins
	 */
	public static String[] getCheckinBody(User user, String url) {
		Token accessToken = new Token(user.accessToken, user.accessTokenSecret);
		OAuthRequest request = new OAuthRequest(Verb.GET, url);
		Application.getConnector().signRequest(accessToken, request);
		Response response = request.send();

		String userCheckins = response.getBody();

		String[] checkins = userCheckins.substring(0, userCheckins.length() - 1).substring(1).split("\\},\\{");

		getCheckins(user, checkins);
		return checkins;
	}

	/**
	 * 
	 * Parses the Checkins from Json
	 * 
	 * @param user
	 *            The Authenticated User
	 * @param checkins
	 *            String Array with Json Checkin Data
	 */
	public static void getCheckins(User user, String[] checkins) {
		for (String checkin : checkins) {
			if (!checkin.startsWith("{")) {
				checkin = "{" + checkin;
			}
			if (checkin.endsWith("}}")) {
				checkin = checkin.substring(0, checkin.length() - 1);
			}

			try {
				checkin = checkin.substring(11);

				MisoCheckinPojo misoCheckin = new Gson().fromJson(checkin, MisoCheckinPojo.class);
				MisoCheckin mc = find(misoCheckin.id, user);

				if (mc == null) {
					Logger.debug("%s found...", checkins.length);
					mc = new MisoCheckin();
					mc.checkin_id = misoCheckin.id;

					DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
					DateTime dt = fmt.parseDateTime(misoCheckin.created_at.replace("Z", ".000Z"));

					mc.isMovie = false;

					if (misoCheckin.episode_label == null) {
						mc.isMovie = true;
					}

					mc.created_at = new Timestamp(dt.getMillis());
					mc.comment = misoCheckin.comment;
					mc.user_username = misoCheckin.user_username;
					mc.user_full_name = misoCheckin.user_full_name;
					mc.user_profile_image_url = misoCheckin.user_profile_image_url;
					mc.media_id = misoCheckin.media_id;
					mc.media_title = misoCheckin.media_title;
					mc.media_poster_url = misoCheckin.media_poster_url;
					mc.media_poster_url_small = misoCheckin.media_poster_url_small;
					mc.episode_num = misoCheckin.episode_num;
					mc.episode_season_num = misoCheckin.episode_season_num;
					mc.episode_label = misoCheckin.episode_label;
					mc.episode_title = misoCheckin.episode_title;
					mc.episode_poster_url = misoCheckin.episode_poster_url;
					mc.episode_poster_url_small = misoCheckin.episode_poster_url_small;
				
					mc.user = user;
					mc.save();

				}

			} catch (Exception e) {
				// TODO: handle exception
			}

		}
	}

	/**
	 * Has the user any Chekins
	 * 
	 * @return true or false
	 */
	public static Boolean hasCheckins() {
		if (MisoCheckin.find("order by checkin_id asc").first() == null) {
			return false;
		}
		return true;
	}

	/**
	 * Find one Checkin by the given checkin_id
	 * 
	 * @param user
	 *            The Authenticated User
	 * @param checkin_id
	 *            MisoCheckin.checkin_id
	 * @return Object MisoCheckin or Null if nothing found
	 */
	public static MisoCheckin find(Long checkin_id, User user) {
		return MisoCheckin.find("checkin_id = ? and user_id = ?", checkin_id, user.id).first();
	}

	/**
	 * Try to get the oldest Checkin for the given user
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return Object MisoCheckin or Null if nothing found
	 */
	public static MisoCheckin findFirst(User user) {
		return MisoCheckin.find("user_id = ? order by checkin_id asc", user.id).first();
	}

	/**
	 * Try to get the newest Checkin for the given user
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return Object MisoCheckin or Null if nothing found
	 */
	public static MisoCheckin findLast(User user) {
		return MisoCheckin.find("user_id = ?  order by checkin_id desc", user.id).first();
	}

	/**
	 * Try to get the newest Checkin for the given user
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return Object MisoCheckin or Null if nothing found
	 */
	public static List<MisoCheckin> findLastSeries(User user, int count) {
		// Logger.debug("findLastSeries...");
		List<MisoCheckin> misocheckin = new TreeList();
		ResultSet results = DB.executeQuery("select * from (select max(created_at) as last_checkin, media_id, media_title from MisoCheckin where isMovie = 0 group by media_id, media_title order by created_at desc) msc order by last_checkin desc limit " + count);
		try {
			while (results.next()) {
				misocheckin.add(MisoCheckin.findSeriesEpisode(user, results.getLong("media_id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return misocheckin;
	}

	/**
	 * Try to get the newest Checkin for the given user
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return Object MisoCheckin or Null if nothing found
	 */
	public static List<MisoCheckin> findLastMovies(User user, int count) {
		// Logger.debug("findLastMovies...");
		List<MisoCheckin> misocheckin = new TreeList();
		ResultSet results = DB.executeQuery("select * from (select max(created_at) as last_checkin, media_id, media_title from MisoCheckin where isMovie = 1 group by media_id, media_title order by created_at desc) msc order by last_checkin desc limit " + count);
		try {
			while (results.next()) {
				misocheckin.add(MisoCheckin.findSeriesEpisode(user, results.getLong("media_id")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return misocheckin;
	}

	/**
	 * Find all Series and Episodes
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return List<MisoCheckin> list of Episodes
	 */
	public static List<MisoCheckin> findSeries(User user) {
		return MisoCheckin.find("user_id = ? and isMovie = false order by checkin_id desc group by media_title", user.id).fetch();
	}

	/**
	 * Find all
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return List<MisoCheckin> list of Episodes
	 */
	public static List<MisoCheckin> findAll(User user) {
		return MisoCheckin.find("user_id = ? order by checkin_id desc", user.id).fetch();
	}
	
	/**
	 * Find all
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return List<MisoCheckin> list of Episodes
	 */
	public static List<MisoCheckin> findAll(User user, int limit) {
		return MisoCheckin.find("user_id = ? order by checkin_id desc", user.id).fetch(limit);
	}
	
	/**
	 * Find Series, no Episodes
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return HashMap<String,Long> with the Series
	 */
	public static TreeMap<String, MisoCheckin> findBaseSeries(User user) {
		TreeMap<String, MisoCheckin> sl = new TreeMap<String, MisoCheckin>();
		List<MisoCheckin> mc = MisoCheckin.find("user_id = ? and isMovie = false order by media_title, episode_label", user.id).fetch();
		for (MisoCheckin misoCheckin : mc) {
			sl.put(misoCheckin.media_title, misoCheckin);
		}
		return sl;
	}

	/**
	 * get the SeriesDetails
	 * 
	 * @param user
	 *            The Authenticated User
	 * @param media_id
	 * @return Object MisoCheckin or Null if nothing found
	 */
	public static MisoCheckin findSeriesEpisode(User user, Long media_id) {
		return MisoCheckin.find("user_id = ? and media_id = ? order by media_title, episode_label desc", user.id, media_id).first();
	}

	/**
	 * Get all Episodes from an Series
	 * 
	 * @param user
	 *            The Authenticated User
	 * @param media_id
	 * @return Object MisoCheckin or Null if nothing found
	 */
	public static List<MisoCheckin> findSeriesEpisodes(User user, Long media_id) {
		return MisoCheckin.find("user_id = ? and media_id = ? order by media_title, episode_label desc", user.id, media_id).fetch();
	}

	public static MisoCheckin findByCheckinId(Long checkinId) {
		return MisoCheckin.find("checkin_id = ?", checkinId).first();
		
	}
}
