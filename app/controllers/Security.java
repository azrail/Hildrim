package controllers;

import models.*;

public class Security extends Secure.Security {

	static boolean authenticate(String username, String password) {
		return User.connect(username, password) != null;
	}

	static boolean check(String profile) {
		if (User.find("byEmail", connected()).<User> first() == null) {
			System.out.println("not auhtenticated");
			return true;
		}
		
		System.out.println(User.find("byEmail", connected()).<User> first());
		
		return false;
	}

	static void onDisconnected() {
		Application.index(null);
	}

	static void onAuthenticated() {
		Application.index(null);
	}
}
