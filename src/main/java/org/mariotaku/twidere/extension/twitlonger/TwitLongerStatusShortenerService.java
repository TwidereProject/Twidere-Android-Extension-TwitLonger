package org.mariotaku.twidere.extension.twitlonger;

import android.accounts.Account;
import android.support.annotation.Nullable;
import android.util.Log;

import org.mariotaku.twidere.Twidere;
import org.mariotaku.twidere.annotation.AccountType;
import org.mariotaku.twidere.model.AccountDetails;
import org.mariotaku.twidere.model.ParcelableStatus;
import org.mariotaku.twidere.model.ParcelableStatusUpdate;
import org.mariotaku.twidere.model.StatusShortenResult;
import org.mariotaku.twidere.model.UserKey;
import org.mariotaku.twidere.service.StatusShortenerService;

/**
 * Tweet shortener example
 *
 * @author mariotaku
 */
public class TwitLongerStatusShortenerService extends StatusShortenerService implements Constants {

    /**
     * @return Shortened tweet.
     */
    @Override
    protected StatusShortenResult shorten(final ParcelableStatusUpdate status,
                                          final UserKey currentAccountKey,
                                          final String overrideStatusText) {
        final AccountDetails details;
        try {
            details = getOAuthCredentials(currentAccountKey);
        } catch (SecurityException e) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, e);
            }
            return StatusShortenResult.error(-1, getString(R.string.permission_not_granted));
        }
        if (details == null || !isTwitter(details)) {
            return StatusShortenResult.error(-1, "No valid Twitter account found");
        }
        final TwitLonger tl = TwitLongerFactory.getInstance(TWITLONGER_API_KEY, details);
        try {
            final String text;
            if (overrideStatusText != null) {
                text = overrideStatusText;
            } else {
                text = status.text;
            }
            NewPost newPost = new NewPost(text);
            if (status.in_reply_to_status != null) {
                final long inReplyToId = parseLong(status.in_reply_to_status.id);
                final String inReplyToScreenName = status.in_reply_to_status.user_screen_name;
                if (inReplyToId != -1 && inReplyToScreenName != null) {
                    newPost.setInReplyTo(inReplyToId, inReplyToScreenName);
                }
            }
            final Post response = tl.createPost(newPost);
            if (response != null) {
                final StatusShortenResult shortened = StatusShortenResult.shortened(response.tweetContent);
                shortened.extra = response.id;
                return shortened;
            }
        } catch (final TwitLongerException e) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, e);
            }
            return StatusShortenResult.error(-1, e.getMessage());
        }
        return StatusShortenResult.error(-1, "Unknown error");
    }

    private boolean isTwitter(AccountDetails credentials) {
        return AccountType.TWITTER.equals(credentials.type);
    }

    private long parseLong(String str) {
        if (str == null) return -1;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected boolean callback(StatusShortenResult result, ParcelableStatus status) {
        if (result.extra == null) return false;
        final AccountDetails details;
        try {
            details = getOAuthCredentials(status.account_key);
        } catch (SecurityException e) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, e);
            }
            return false;
        }
        final TwitLonger tl = TwitLongerFactory.getInstance(TWITLONGER_API_KEY, details);
        final long statusId = parseLong(status.id);
        if (statusId == -1) return false;
        try {
            tl.updatePost(result.extra, statusId);
        } catch (TwitLongerException e) {
            if (BuildConfig.DEBUG) {
                Log.w(LOGTAG, e);
            }
            return false;
        }
        return true;
    }

    @Nullable
    private AccountDetails getOAuthCredentials(UserKey accountKey) {
        Account account = Twidere.findByAccountKey(this, accountKey);
        if (account == null) return null;
        return Twidere.getAccountDetails(this, account);
    }

}
