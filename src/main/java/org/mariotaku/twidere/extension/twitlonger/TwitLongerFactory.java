package org.mariotaku.twidere.extension.twitlonger;

import android.support.annotation.Nullable;

import org.mariotaku.restfu.ExceptionFactory;
import org.mariotaku.restfu.RestAPIFactory;
import org.mariotaku.restfu.RestRequest;
import org.mariotaku.restfu.annotation.method.GET;
import org.mariotaku.restfu.http.Endpoint;
import org.mariotaku.restfu.http.HttpRequest;
import org.mariotaku.restfu.http.HttpResponse;
import org.mariotaku.restfu.http.ValueMap;
import org.mariotaku.restfu.oauth.OAuthAuthorization;
import org.mariotaku.restfu.oauth.OAuthEndpoint;
import org.mariotaku.restfu.oauth.OAuthToken;
import org.mariotaku.restfu.urlconnection.URLConnectionRestClient;
import org.mariotaku.twidere.model.AccountDetails;
import org.mariotaku.twidere.model.account.cred.OAuthCredentials;


/**
 * Created by mariotaku on 16/2/20.
 */
public class TwitLongerFactory {

    public static TwitLonger getInstance(final String apiKey, @Nullable final AccountDetails credentials) {
        final RestAPIFactory<TwitLongerException> factory = new RestAPIFactory<>();
        factory.setEndpoint(new Endpoint("http://api.twitlonger.com/"));
        factory.setHttpClient(new URLConnectionRestClient());
        factory.setConstantPool(new TwitLongerConstantPool(apiKey, credentials));
        factory.setExceptionFactory(new TwitLongerExceptionFactory());
        factory.setRestConverterFactory(new LoganSquareConverterFactory());
        return factory.build(TwitLonger.class);
    }

    private static class TwitLongerConstantPool implements ValueMap {
        private final String mApiKey;
        private final OAuthAuthorization mAuthorization;
        private final OAuthEndpoint mEndpoint;
        private final RestRequest mRequest;

        public TwitLongerConstantPool(String apiKey, @Nullable AccountDetails accountDetails) {
            mApiKey = apiKey;
            if (accountDetails != null && accountDetails.credentials instanceof OAuthCredentials) {
                final OAuthCredentials credentials = (OAuthCredentials) accountDetails.credentials;
                final OAuthToken accessToken = new OAuthToken(credentials.access_token,
                        credentials.access_token_secret);
                mAuthorization = new OAuthAuthorization(credentials.consumer_key,
                        credentials.consumer_secret, accessToken);
                mEndpoint = new OAuthEndpoint("https://api.twitter.com/1.1/");
                mRequest = new RestRequest(GET.METHOD, false, "/account/verify_credentials.json", null,
                        null, null, null, null, null);
            } else {
                mAuthorization = null;
                mEndpoint = null;
                mRequest = null;
            }
        }

        @Override
        public boolean has(String key) {
            switch (key) {
                case "tl_api_key":
                    return true;
                case "oauth_echo_authorization":
                    return mAuthorization != null;
            }
            return false;
        }

        @Override
        public Object get(String key) {
            switch (key) {
                case "tl_api_key": {
                    return mApiKey;
                }
                case "oauth_echo_authorization": {
                    if (mAuthorization != null) {
                        return mAuthorization.getHeader(mEndpoint, mRequest);
                    }
                }
            }
            return null;
        }

        @Override
        public String[] keys() {
            if (mAuthorization == null) return new String[]{"oauth_echo_authorization"};
            return new String[]{"tl_api_key", "oauth_echo_authorization"};
        }
    }

    private static class TwitLongerExceptionFactory implements ExceptionFactory<TwitLongerException> {
        @Override
        public TwitLongerException newException(Throwable throwable, HttpRequest httpRequest, HttpResponse httpResponse) {
            TwitLongerException exception;
            if (throwable != null) {
                exception = new TwitLongerException(throwable);
            } else {
                exception = new TwitLongerException();
            }
            exception.setRequest(httpRequest);
            exception.setResponse(httpResponse);
            return exception;
        }
    }
}
