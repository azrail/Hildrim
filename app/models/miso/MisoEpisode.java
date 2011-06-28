package models.miso;

import java.sql.Date;

import java.util.List;
import java.util.TreeMap;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.OneToOne;

import com.google.gson.Gson;

import controllers.Application;
import controllers.Series;

import play.Logger;


import javax.persistence.Entity;
import javax.persistence.Lob;

import models.User;


import play.data.validation.MaxSize;
import play.db.jpa.Model;

/**
 * Details of an specified Episode
 * 
 * @author prime
 * 
 */
@Entity

public class MisoEpisode extends Model implements Comparable<MisoEpisode> {
	/**
	 * The media id for this checkin. 5678
	 */

	public Long				media_id;
	/**
	 * The tile for this episode object The Big One
	 */
	public String			title;
	/**
	 * The season during which this episode aired 3
	 */
	public Long				season_num;	
	/**
	 * Checkin Count
	 */
	public Long				checkins;
	
	/**
	 * The season & episode label for the given episode S02E12
	 */
	public String			label;

	public Long		episode_num;
	/**
	 * The date the episode originally aired 2010-12-11T00:21:44Z
	 */
	public String	aired;
	/**
	 * The tvdb id for the given episode 12345
	 */
	public Long		tvdb_id;

	/**
	 * The poster image for this episode object
	 * http://gomiso.com/uploads/BAhbCFsHOgZm.png
	 */
	public String			poster_image_url;
	/**
	 * The thumbnail poster image for this episode object
	 * http://gomiso.com/uploads/JHDkcDlskS.png
	 */
	public String			poster_image_url_small;
	/**
	 * A short text description/synopsis of the episode Leo meets his real
	 * parents and is terrified to learn his father has halitosis.
	 */
	@Lob
	@MaxSize(10000)

	public String			summary;
	/**
	 * Comma-delimited actors related to this media. Christian Bale, Heath
	 * Ledger
	 */
	@Lob
	@MaxSize(10000)
	public String			cast;

	/**
	 * The date the episode originally aired isodate
	 */
	public java.util.Date	aired_date;

	public static MisoEpisode findEpisode(Long media_id, Long episode, Long season) {
		return MisoEpisode.find("media_id = ? and episode_num = ? and season_num = ?", media_id, episode, season).first();
	}

	/**
	 * @param media_id
	 * @param user
	 * @param episode
	 * @param season
	 */
	public static MisoEpisode getEpisodeDetails(Long media_id, User user, Long episode, Long season) {

		MisoEpisode misoEpisode = findEpisode(media_id, episode, season);

		if (misoEpisode == null) {
			String episodeBody = Application.getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/episodes/show.json?media_id=" + media_id + "&season_num=" + season + "&episode_num=" + episode, Series.GET);
			if (episodeBody.contains("Episode not found")) {
				
				misoEpisode = findEpisode(media_id, episode + 1L, season);
				
				if (misoEpisode != null) {
					return misoEpisode;
				}
				
				season = season + 1L;
				episode = 1L;

				misoEpisode = findEpisode(media_id, episode, season);
				
				if (misoEpisode == null) {
					episodeBody = Application.getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/episodes/show.json?media_id=" + media_id + "&season_num=" + season + "&episode_num=" + episode, Series.GET);
				} else {
					return misoEpisode;
				}
			}

			if (episodeBody.contains("Episode not found")) {
				return null;
			}

			episodeBody = episodeBody.substring(11, episodeBody.length() - 1);
			misoEpisode = new Gson().fromJson(episodeBody, MisoEpisode.class);
			misoEpisode.media_id = media_id;
			misoEpisode.save();
			
			Logger.debug("%s - %s with id %s saved", misoEpisode.label, misoEpisode.title, misoEpisode.id);
			return misoEpisode;

		} else {
			return misoEpisode;
		}

	}

	/**
	 * Find Episodes by Season
	 * 
	 * @param user
	 *            The Authenticated User
	 * @return HashMap<String,Long> with the Series
	 */
	public static List<MisoEpisode> findEpisodes(Long media_id, Long season) {
		return MisoEpisode.find("media_id = ? and season_num = ? order by episode_num", media_id, season).fetch();
	}
	
	@Override
	public int compareTo(MisoEpisode otherMisoEspisode) {

		if (otherMisoEspisode.media_id == media_id && otherMisoEspisode.season_num == season_num && otherMisoEspisode.episode_num == episode_num) {
			return 0;
		} else {
			return -1;
		}
	}

	public String toString() {
		return "media_id[" + media_id + "],title[" + title + "],season_num[" + season_num + "],episode_num[" + episode_num + "],label[" + label + "]";
	}

}
