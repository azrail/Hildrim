package helpers.miso;

import models.miso.MisoEpisode;


/**
 * Helper Class for Json deserialisation with gson, represents an Miso Checkin
 * 
 * @author prime
 * 
 */
public class MisoMediaPojo {
	//id	The id for this media object.	5678
	public Long id;
	//title	The media title for this object.	The Dark Knight
	public String title;
	//poster_image_url	The poster image for this media object.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	public String poster_image_url;
	//poster_image_url_small	The thumbnail poster image for this media object.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	public String poster_image_url_small;
	//kind	The media object's type.	'TvShow' or 'Movie'
	public String kind;
	//release_year	The year the movie was released or the show started.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	public String release_year;
	//tvdb_id	The tvdb id associated with this media, if it exists.	tt1234
	public String tvdb_id;
	//currently_favorited	The media is currently favorited by the logged in user.	true
	public Boolean currently_favorited;
	//episode_count	The total number of episodes aired for this media (if it's a TV Show)	123
	public Long episode_count;
	//latest_episode
	public MisoEpisode latest_episode;
	//"summary": "Ally McBeal is an American comedy-drama series which aired on the Fox network from 1997 to 2002. The series was created by David E. Kelley, who also served as the executive producer, along with Bill D'Elia. The series stars Calista Flockhart in the title role as a young lawyer working in the fictional Boston law firm Cage and Fish with other young lawyers whose lives and loves were eccentric, humorous and dramatic.\n\n",
	public String summary;
	//"genres":
	public String genres;
	//"cast"
	public String cast;
}
