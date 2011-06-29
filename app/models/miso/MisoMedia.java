package models.miso;

import helpers.miso.MisoMediaPojo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import models.User;
import play.Logger;
import play.data.validation.MaxSize;
import play.db.DB;
import play.db.jpa.Model;
import play.libs.F.Promise;

import com.google.gson.Gson;

import controllers.Application;
import controllers.Series;

/**
 * Details of an specified Movie
 * 
 * @author prime
 * 
 */
@Entity

public class MisoMedia extends Model implements Comparable<MisoMedia> {

	//id	The id for this media object.	5678
	Long media_id;
	//title	The media title for this object.	The Dark Knight
	String title;
	//poster_image_url	The poster image for this media object.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	@Lob
	@MaxSize(10000)
	String poster_image_url;
	//poster_image_url_small	The thumbnail poster image for this media object.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	@Lob
	@MaxSize(10000)
	String poster_image_url_small;
	//kind	The media object's type.	'TvShow' or 'Movie'
	String kind;
	//release_year	The year the movie was released or the show started.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	String release_year;
	//tvdb_id	The tvdb id associated with this media, if it exists.	tt1234
	String tvdb_id;
	//currently_favorited	The media is currently favorited by the logged in user.	true
	Boolean currently_favorited;
	//episode_count	The total number of episodes aired for this media (if it's a TV Show)	123
	Long episode_count;
	//latest_episode
	@OneToOne
	public MisoEpisode latest_episode;
	@OneToOne
	public MisoSeries series;
	@Lob
	@MaxSize(10000)
	public String summary;
	//"genres":
	@Lob
	@MaxSize(10000)
	public String genres;
	//"cast"
	@Lob
	@MaxSize(10000)
	public String cast;
	
	public Boolean isMovie = false;

	public static MisoMedia findMedia(Long media_id) {
		return MisoMedia.find("media_id = ?", media_id).first();
	}

	
	public static void getMediaDetails(User user) {
		ResultSet results = DB.executeQuery("select media_id from MisoCheckin group by media_id");
		try {
			while (results.next()) {
				Long media_id = results.getLong("media_id");
				getMediaDetails(media_id, user);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * @param media_id
	 * @param user
	 * @param episode
	 * @param season
	 */
	public static MisoMedia getMediaDetails(Long media_id, User user) {

		MisoMedia misoMedia = findMedia(media_id);
		
		if (misoMedia == null) {
			String episodeBody = Application.getJsonBodyforUrl(user, "http://gomiso.com/api/oauth/v1/media/show.json?media_id=" + media_id, Series.GET);

			if (episodeBody.contains("Episode not found")) {
				return null;
			}

			episodeBody = episodeBody.substring(9, episodeBody.length() - 1);			
			
			MisoMediaPojo misoMediaPojo = new Gson().fromJson(episodeBody, MisoMediaPojo.class);
			
			misoMedia = new MisoMedia();
			misoMedia.media_id = misoMediaPojo.id;
			misoMedia.currently_favorited = misoMediaPojo.currently_favorited;
			misoMedia.episode_count = misoMediaPojo.episode_count;
			misoMedia.kind = misoMediaPojo.kind;
			misoMedia.poster_image_url = misoMediaPojo.poster_image_url;
			misoMedia.poster_image_url_small = misoMediaPojo.poster_image_url_small;
			misoMedia.title = misoMediaPojo.title;
			misoMedia.tvdb_id = misoMediaPojo.tvdb_id;
			misoMedia.release_year = misoMediaPojo.release_year;
			misoMedia.summary = misoMediaPojo.summary;
			misoMedia.genres = misoMediaPojo.genres;
			misoMedia.cast = misoMediaPojo.cast;
			
			if (misoMedia.kind.equals("Movie")) {
				misoMedia.isMovie = true;
			}
			
			if (misoMediaPojo.latest_episode != null) {
				MisoEpisode misoEpisode = MisoEpisode.getEpisodeDetails(misoMediaPojo.latest_episode.media_id, user, misoMediaPojo.latest_episode.episode_num, misoMediaPojo.latest_episode.season_num);
				misoMedia.latest_episode = misoEpisode;
				misoMedia.latest_episode.save();
			}

			
			if (!misoMedia.isMovie) {
				MisoSeries misoSeries = MisoSeries.getSeriesDetails(media_id, user);
				misoMedia.series = misoSeries;
			}
			
			misoMedia.save();
			
			Logger.debug("%s with id %s saved", misoMedia.title, misoMedia.media_id);
			return misoMedia;

		} else {
			return misoMedia;
		}

	}
	
	@Override
	public int compareTo(MisoMedia otherMisoEspisode) {

		if (otherMisoEspisode.id == id) {
			return 0;
		} else {
			return -1;
		}
	}

	public String toString() {
		return "media_id[" + id + "],title[" + title + "]";
	}

}
