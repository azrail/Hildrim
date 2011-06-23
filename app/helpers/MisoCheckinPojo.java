package helpers;


public class MisoCheckinPojo {
	public Long id; //	The id for this checkin object.	9876
	public String created_at; //	The timestamp for when this checkin was created	"2010-12-11T00:21:44Z"
	public String comment; //	The comment attached for this checkin.	Dexter is a great show
	public Long user_id; //	The user id for this checkin.	1234
	public String user_username; //	The user username for this checkin.	john24
	public String user_full_name; //	The user name for this checkin.	John Smith
	public String user_profile_image_url; //	The profile image for the given user.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	public Long media_id; //	The media id for this checkin.	5678
	public String media_title; //	The media title for this checkin.	The Dark Knight
	public String media_poster_url; //	The poster image for the given media.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	public String media_poster_url_small; //	The thumbnail poster image for the given media.	http://gomiso.com/uploads/BAhbCFsHOgZm.png
	public Long episode_num; //	The number of the episode checked into (if this is an episode checkin)	12
	public Long episode_season_num; //	The number of the season the episode belonged to (if this is an episode checkin)	12
	public String episode_label; //	The label of the episode checked into (if this is an episode checkin)	S03E12
	public String episode_title; //	The title of the episode checked into (if this is an episode checkin)	It Takes Two to Tango
	public String episode_poster_url; //	The image url of the episode checked into (if this is an episode checkin)	http://gomiso.com/abcdefg.jpg
	public String episode_poster_url_small; //The thumbnail image url of the episode checked into (if this is an episode checkin) http://gomiso.com/abcdefg.jpg
}
