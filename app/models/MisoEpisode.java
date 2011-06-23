package models;

import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.Lob;

import play.data.validation.MaxSize;
import play.db.jpa.Model;

/**
 * Details of an specified Episode
 * 
 * @author prime
 * 
 */
@Entity
public class MisoEpisode extends Model {

	/**
	 * The media id for this checkin. 5678
	 */
	public Long		media_id;
	/**
	 * The tile for this episode object The Big One
	 */
	public String	title;
	/**
	 * The season during which this episode aired 3
	 */
	public Long		season_num;
	/**
	 * The episode number with respect to the season during which this episode
	 * aired 12
	 */
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
	 * The season & episode label for the given episode S02E12
	 */
	public String	label;
	/**
	 * The poster image for this episode object
	 * http://gomiso.com/uploads/BAhbCFsHOgZm.png
	 */
	public String	poster_image_url;
	/**
	 * The thumbnail poster image for this episode object
	 * http://gomiso.com/uploads/JHDkcDlskS.png
	 */
	public String	poster_image_url_small;
	/**
	 * A short text description/synopsis of the episode Leo meets his real
	 * parents and is terrified to learn his father has halitosis.
	 */
	@Lob
	@MaxSize(10000)
	public String	summary;
	/**
	 * Comma-delimited actors related to this media. Christian Bale, Heath
	 * Ledger
	 */
	@Lob
	@MaxSize(10000)
	public String	cast;

	public static MisoEpisode findEpisode(Long media_id, Long episode, Long season) {
		return MisoEpisode.find("media_id = ? and episode_num = ? and season_num = ?", media_id, episode, season).first();
	}

}
