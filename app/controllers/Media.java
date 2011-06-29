package controllers;

import models.miso.MisoCheckin;
import play.mvc.Controller;

public class Media extends Controller {

	public static String	TEMPLATEPATH	= "Media/";

	public static void checkin(Long checkinId) {
		MisoCheckin misoCheckin = MisoCheckin.findByCheckinId(checkinId);
		render(Application.getTemplate(TEMPLATEPATH, "checkin.html"), misoCheckin);
	}
}
