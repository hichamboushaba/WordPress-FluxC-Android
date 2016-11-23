package org.wordpress.android.fluxc.release;

import org.apache.commons.lang.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailErrorType;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthEmailSent;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_AccountTest extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    CountDownLatch mCountDownLatch;

    private enum ACCOUNT_TEST_ACTIONS {
        NONE,
        AUTHENTICATE,
        INCORRECT_USERNAME_OR_PASSWORD_ERROR,
        AUTHENTICATE_2FA_ERROR,
        FETCHED,
        POSTED,
        SENT_AUTH_EMAIL,
        AUTH_EMAIL_ERROR_INVALID,
        AUTH_EMAIL_ERROR_NO_SUCH_USER
    }

    private ACCOUNT_TEST_ACTIONS mExpectedAction;
    private boolean mExpectAccountInfosChanged;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register
        mDispatcher.register(this);
        mExpectedAction = ACCOUNT_TEST_ACTIONS.NONE;
    }

    public void testWPComAuthenticationOK() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
    }

    public void testWPComAuthenticationIncorrectUsernameOrPassword() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.INCORRECT_USERNAME_OR_PASSWORD_ERROR;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_BAD_PASSWORD);
    }

    public void testWPCom2faAuthentication() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE_2FA_ERROR;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_2FA, BuildConfig.TEST_WPCOM_PASSWORD_2FA);
    }

    public void testWPComFetch() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }
        mExpectedAction = ACCOUNT_TEST_ACTIONS.FETCHED;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        mCountDownLatch = new CountDownLatch(2);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWPComPost() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }
        mExpectedAction = ACCOUNT_TEST_ACTIONS.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = String.valueOf(System.currentTimeMillis());
        mExpectAccountInfosChanged = true;
        payload.params = new HashMap<>();
        payload.params.put("description", newValue);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, mAccountStore.getAccount().getAboutMe());
    }

    public void testWPComPostNoChange() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mExpectedAction = ACCOUNT_TEST_ACTIONS.FETCHED;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        mCountDownLatch = new CountDownLatch(2);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mExpectedAction = ACCOUNT_TEST_ACTIONS.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = mAccountStore.getAccount().getAboutMe();
        mExpectAccountInfosChanged = false;
        payload.params = new HashMap<>();
        payload.params.put("description", newValue);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, mAccountStore.getAccount().getAboutMe());
    }

    public void testWPComPostPrimarySiteIdNoChange() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mExpectedAction = ACCOUNT_TEST_ACTIONS.FETCHED;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        mCountDownLatch = new CountDownLatch(2);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mExpectedAction = ACCOUNT_TEST_ACTIONS.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = String.valueOf(mAccountStore.getAccount().getPrimarySiteId());
        mExpectAccountInfosChanged = false;
        payload.params = new HashMap<>();
        payload.params.put("primary_site_ID", newValue);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, String.valueOf(mAccountStore.getAccount().getPrimarySiteId()));
    }

    public void testSendAuthEmail() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.SENT_AUTH_EMAIL;
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(BuildConfig.TEST_WPCOM_EMAIL_TEST1));
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailInvalid() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTH_EMAIL_ERROR_INVALID;
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction("notanemail"));
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailNoSuchUser() throws InterruptedException {
        mExpectedAction = ACCOUNT_TEST_ACTIONS.AUTH_EMAIL_ERROR_NO_SUCH_USER;
        String unknownEmail = "marty" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + "@themacflys.com";
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(unknownEmail));
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            switch (mExpectedAction) {
                case AUTHENTICATE_2FA_ERROR:
                    assertEquals(event.error.type, AuthenticationErrorType.NEEDS_2FA);
                    break;
                case INCORRECT_USERNAME_OR_PASSWORD_ERROR:
                    assertEquals(event.error.type, AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD);
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.AUTHENTICATE);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.FETCHED);
            assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.FETCHED);
            assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.PUSH_SETTINGS) {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.POSTED);
            assertEquals(mExpectAccountInfosChanged, event.accountInfosChanged);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthEmailSent(OnAuthEmailSent event) {
        AppLog.i(AppLog.T.API, "Received OnAuthEmailSent");
        if (event.isError()) {
            AppLog.i(AppLog.T.API, "OnAuthEmailSent has error: " + event.error.type + " - " + event.error.message);
            if (event.error.type == AuthEmailErrorType.INVALID_INPUT) {
                assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.AUTH_EMAIL_ERROR_INVALID);
                mCountDownLatch.countDown();
            } else if (event.error.type == AuthEmailErrorType.NO_SUCH_USER) {
                assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.AUTH_EMAIL_ERROR_NO_SUCH_USER);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else {
            assertEquals(mExpectedAction, ACCOUNT_TEST_ACTIONS.SENT_AUTH_EMAIL);
            mCountDownLatch.countDown();
        }
    }

    private void authenticate(String username, String password) throws InterruptedException {
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
