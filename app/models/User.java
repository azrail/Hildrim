package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import play.data.validation.Email;
import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
public class User extends Model {

	public String		username;

	@Email
	@Required
	public String		email;

	@Required
	public String		password;

	public String		requestToken;
	public String		authUrl;
	public String		oauth_verifier;
	public String		oauth_token;
	public String		requestTokenSecret;
	public String		accessToken;
	public String		accessTokenSecret;
	public Timestamp	lastupdate;

	@OneToOne
	public MisoUser			miso;
	
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	public List<MisoCheckin>	misoCheckins;
	

	public User(String email, String password, String username) {
		this.email = email;
		this.password = password;
		this.username = username;
		this.misoCheckins = new ArrayList<MisoCheckin>();
		this.miso = new MisoUser();
		this.miso.save();
	}

	public static User find(String email) {
		return User.find("byEmail", email).first();
	}

	public static User connect(String email, String password) {
		return find("byEmailAndPassword", email, password).first();
	}

}
