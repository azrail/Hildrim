package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.google.gson.Gson;

import controllers.Application;

import play.data.validation.Email;
import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class User extends Model {

	public String				username;

	@Email
	@Required
	public String				email;

	@Required
	public String				password;

	public String				requestToken;
	public String				authUrl;
	public String				oauth_verifier;
	public String				oauth_token;
	public String				requestTokenSecret;
	public String				accessToken;
	public String				accessTokenSecret;
	public Timestamp			lastupdate;

	@OneToOne(cascade = CascadeType.ALL)
	public MisoUser				miso;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	public List<MisoCheckin>	misoCheckins;

	public User(String email, String password, String username) {
		this.email = email;
		this.password = password;
		this.username = username;
		this.misoCheckins = new ArrayList<MisoCheckin>();
	}

	public static User find(String email) {
		return User.find("byEmail", email).first();
	}

	public static User connect(String email, String password) {
		return find("byEmailAndPassword", email, password).first();
	}

	public static User findbyMisoUserID(Long miso_id) {
		return User.find("byMiso_id", miso_id).first();
	}

	/**
	 * @param user
	 */
	public static User updateMisoUserDetails(User user) {
		DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.0");
		DateTime dtlast = null;
		if (user.lastupdate == null) {
			dtlast = fmt.parseDateTime("1970-01-01 12:00:00.0");
		} else {
			dtlast = fmt.parseDateTime(user.lastupdate.toString());
		}

		DateTime dtcur = new DateTime();

		if (user.miso == null || user.lastupdate == null || dtlast.isBefore(dtcur.minusHours(2))) {
			Token accessToken = new Token(user.accessToken, user.accessTokenSecret);
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://gomiso.com/api/oauth/v1/users/show.json");
			Application.getConnector().signRequest(accessToken, request);
			Response response = request.send();

			String userdetails = response.getBody().substring(8);

			System.out.println(userdetails);

			userdetails = userdetails.substring(0, userdetails.length() - 1);

			System.out.println(userdetails);

			MisoUser m = new Gson().fromJson(userdetails, MisoUser.class);

			User u = findbyMisoUserID(m.id);

			if (findbyMisoUserID(m.id) == null) {
				user.miso = new MisoUser();
				user.miso.id = m.id;
				user.miso.full_name = m.full_name;
				user.miso.currently_followed = m.currently_followed;
				user.miso.follower_count = m.follower_count;
				user.miso.tagline = m.tagline;
				user.miso.facebook_enabled = m.facebook_enabled;
				user.miso.following_count = m.following_count;
				user.miso.badge_count = m.badge_count;
				user.miso.twitter_enabled = m.twitter_enabled;
				user.miso.checkin_count = m.checkin_count;
				user.miso.username = m.username;
				user.miso.total_points = m.total_points;
				user.miso.url = m.url;
				user.miso.profile_image_url = m.profile_image_url;
				user.miso.facebook = m.facebook;
				user.miso.twitter = m.twitter;
				user.miso.save();

				user.username = m.username;
				user.lastupdate = new Timestamp(System.currentTimeMillis());
				user.save();
			} else {
				user.miso = new MisoUser();
				user.miso.id = m.id;
				user.miso.full_name = m.full_name;
				user.miso.currently_followed = m.currently_followed;
				user.miso.follower_count = m.follower_count;
				user.miso.tagline = m.tagline;
				user.miso.facebook_enabled = m.facebook_enabled;
				user.miso.following_count = m.following_count;
				user.miso.badge_count = m.badge_count;
				user.miso.twitter_enabled = m.twitter_enabled;
				user.miso.checkin_count = m.checkin_count;
				user.miso.username = m.username;
				user.miso.total_points = m.total_points;
				user.miso.url = m.url;
				user.miso.profile_image_url = m.profile_image_url;
				user.miso.facebook = m.facebook;
				user.miso.twitter = m.twitter;

				user.accessToken = u.accessToken;
				user.accessTokenSecret = u.accessTokenSecret;
				user.authUrl = u.authUrl;
				user.email = u.email;
				user.oauth_token = u.oauth_token;
				user.oauth_verifier = u.oauth_verifier;
				user.misoCheckins = u.misoCheckins;
				user.requestToken = u.requestToken;
				user.requestTokenSecret = u.requestTokenSecret;
				user.username = m.username;
				user.email = u.email;
				user.password = u.password;
				user.lastupdate = new Timestamp(System.currentTimeMillis());
				u.delete();
				user.miso.save();
				user.save();
			}

		}
		
		return user;
		
	}

}
