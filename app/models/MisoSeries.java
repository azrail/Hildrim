package models;

import javax.persistence.Entity;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.Logger;
import play.db.jpa.Model;

import com.google.gson.Gson;

import controllers.Application;

/**
 * Details of an specified Series
 * @author prime
 *
 */
@Entity
public class MisoSeries extends Model {

	/**
	 * Base Episodes URL
	 */
	private static String	EPISODESURL	= "http://gomiso.com/api/oauth/v1/episodes.json?media_id=";
	/**
	 * The media id for this checkin. 5678
	 */
	public Long				media_id;
	/**
	 * The total number of episodes for the given media item 136
	 */
	public Long				episode_count;
	/**
	 * The total number of seasons for the given media item 7
	 */
	public Long				season_count;
	/**
	 * The seasons for which we have episodes for the given media item (for
	 * cases where there is a season 0 and other anomalies) [0, 1, 2, 6]
	 */
	public String[]			seasons;

	/**
	 * @param media_id
	 *            The media id for this checkin. 5678
	 * @param user
	 *            The Authenticated User
	 * @return MisoSeries Series Details
	 */
	public static MisoSeries getSeriesDetails(Long media_id, User user) {
		if (!hasSeriesData(media_id)) {
			String strEpisodes = getJsonBodyforUrl(user, EPISODESURL + media_id);
			MisoSeries misoSeries = new Gson().fromJson(strEpisodes, MisoSeries.class);
			misoSeries.media_id = media_id;
			misoSeries.save();
			return misoSeries;
		} else {
			return MisoSeries.find("byMedia_id", media_id).first();
		}
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
	public static String getJsonBodyforUrl(User user, String url) {
		Logger.debug("Fetching %s", url);
		Token accessToken = new Token(user.accessToken, user.accessTokenSecret);
		OAuthRequest request = new OAuthRequest(Verb.GET, url);
		Application.getConnector().signRequest(accessToken, request);
		Response response = request.send();
		return response.getBody();
	}

	/**
	 * Has the Series any Data
	 * 
	 * @return true or false
	 */
	public static Boolean hasSeriesData(Long media_id) {
		if (MisoSeries.find("byMedia_id", media_id).first() == null) {
			return false;
		}
		return true;
	}
}
