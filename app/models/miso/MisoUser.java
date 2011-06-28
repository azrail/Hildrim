package models.miso;

import java.sql.Timestamp;
import java.util.HashMap;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.jpa.GenericModel;

@Entity
public class MisoUser extends GenericModel {
	@Id
	public Long					id;
	public String					full_name;
	public Boolean					currently_followed;
	public Integer					follower_count;
	public String					tagline;
	public Boolean					facebook_enabled;
	public Integer					following_count;
	public Integer					badge_count;
	public Boolean					twitter_enabled;
	public Integer					checkin_count;
	public String					username;
	public Integer					total_points;
	public String					url;
	public String					profile_image_url;
	public HashMap<String, String>	facebook;
	public HashMap<String, String>	twitter;
}
