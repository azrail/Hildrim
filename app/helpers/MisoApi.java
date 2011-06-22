package helpers;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class MisoApi extends DefaultApi10a {

	private static final String	AUTHORIZE_URL			= "https:/gomiso.com/oauth/authorize?oauth_token=%s";
	private static final String	REQUEST_TOKEN_RESOURCE	= "gomiso.com/oauth/request_token";
	private static final String	ACCESS_TOKEN_RESOURCE	= "gomiso.com/oauth/access_token";

	@Override
	public String getAccessTokenEndpoint() {
		return "http://" + ACCESS_TOKEN_RESOURCE;
	}

	@Override
	public String getAuthorizationUrl(Token requestToken) {
		return String.format(AUTHORIZE_URL, requestToken.getToken());
	}

	@Override
	public String getRequestTokenEndpoint() {
		return "http://" + REQUEST_TOKEN_RESOURCE;
	}

	public static class SSL extends MisoApi {
		@Override
		public String getAccessTokenEndpoint() {
			return "https://" + ACCESS_TOKEN_RESOURCE;
		}

		@Override
		public String getRequestTokenEndpoint() {
			return "https://" + REQUEST_TOKEN_RESOURCE;
		}
	}
}
