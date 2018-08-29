/*
 * This is the source code of Telegram for Android v. 3.x.x
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MrzRecognizer;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SRPHelper;
import org.telegram.messenger.SecureDocument;
import org.telegram.messenger.SecureDocumentKey;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ContextProgressView;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.HintEditText;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgress;
import org.telegram.ui.Components.SlideView;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;

public class PassportActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public final static int TYPE_REQUEST = 0;
    public final static int TYPE_IDENTITY = 1;
    public final static int TYPE_ADDRESS = 2;
    public final static int TYPE_PHONE = 3;
    public final static int TYPE_EMAIL = 4;
    public final static int TYPE_PASSWORD = 5;
    public final static int TYPE_EMAIL_VERIFICATION = 6;
    public final static int TYPE_PHONE_VERIFICATION = 7;
    public final static int TYPE_MANAGE = 8;

    private final static int FIELD_NAME = 0;
    private final static int FIELD_MIDNAME = 1;
    private final static int FIELD_SURNAME = 2;
    private final static int FIELD_BIRTHDAY = 3;
    private final static int FIELD_GENDER = 4;
    private final static int FIELD_CITIZENSHIP = 5;
    private final static int FIELD_RESIDENCE = 6;
    private final static int FIELD_CARDNUMBER = 7;
    private final static int FIELD_EXPIRE = 8;
    private final static int FIELD_IDENTITY_COUNT = 9;
    private final static int FIELD_IDENTITY_NODOC_COUNT = 7;

    private final static int FIELD_NATIVE_NAME = 0;
    private final static int FIELD_NATIVE_MIDNAME = 1;
    private final static int FIELD_NATIVE_SURNAME = 2;
    private final static int FIELD_NATIVE_COUNT = 3;

    private final static int FIELD_STREET1 = 0;
    private final static int FIELD_STREET2 = 1;
    private final static int FIELD_POSTCODE = 2;
    private final static int FIELD_CITY = 3;
    private final static int FIELD_STATE = 4;
    private final static int FIELD_COUNTRY = 5;
    private final static int FIELD_ADDRESS_COUNT = 6;

    private final static int FIELD_PHONECOUNTRY = 0;
    private final static int FIELD_PHONECODE = 1;
    private final static int FIELD_PHONE = 2;

    private final static int FIELD_EMAIL = 0;

    private final static int FIELD_PASSWORD = 0;

    private final static int UPLOADING_TYPE_DOCUMENTS = 0;
    private final static int UPLOADING_TYPE_SELFIE = 1;
    private final static int UPLOADING_TYPE_FRONT = 2;
    private final static int UPLOADING_TYPE_REVERSE = 3;
    private final static int UPLOADING_TYPE_TRANSLATION = 4;

    private String initialValues;
    private int currentActivityType;
    private int currentBotId;
    private String currentPayload;
    private String currentNonce;
    private boolean useCurrentValue;
    private String currentScope;
    private String currentCallbackUrl;
    private String currentPublicKey;
    private String currentCitizeship = "";
    private String currentResidence = "";
    private String currentGender;
    private int[] currentExpireDate = new int[3];
    private TLRPC.TL_account_authorizationForm currentForm;

    private TLRPC.TL_secureRequiredType currentType;
    private TLRPC.TL_secureRequiredType currentDocumentsType;
    private ArrayList<TLRPC.TL_secureRequiredType> availableDocumentTypes;
    private TLRPC.TL_secureValue currentTypeValue;
    private TLRPC.TL_secureValue currentDocumentsTypeValue;
    private TLRPC.TL_account_password currentPassword;
    private TLRPC.TL_auth_sentCode currentPhoneVerification;

    private ActionBarMenuItem doneItem;
    private AnimatorSet doneItemAnimation;
    private ContextProgressView progressView;

    private TextView acceptTextView;
    private ContextProgressView progressViewButton;
    private FrameLayout bottomLayout;

    private TextSettingsCell uploadDocumentCell;
    private View extraBackgroundView;
    private View extraBackgroundView2;
    private TextDetailSettingsCell uploadFrontCell;
    private TextDetailSettingsCell uploadReverseCell;
    private TextDetailSettingsCell uploadSelfieCell;
    private TextSettingsCell uploadTranslationCell;
    private EditTextBoldCursor[] inputFields;
    private ViewGroup[] inputFieldContainers;
    private EditTextBoldCursor[] inputExtraFields;
    private ScrollView scrollView;
    private LinearLayout linearLayout2;
    private LinearLayout documentsLayout;
    private LinearLayout frontLayout;
    private LinearLayout reverseLayout;
    private LinearLayout selfieLayout;
    private LinearLayout translationLayout;
    private LinearLayout currentPhotoViewerLayout;
    private HeaderCell headerCell;
    private ArrayList<View> dividers = new ArrayList<>();
    private ShadowSectionCell sectionCell;
    private ShadowSectionCell sectionCell2;
    private TextInfoPrivacyCell bottomCell;
    private TextInfoPrivacyCell bottomCellTranslation;
    private TextInfoPrivacyCell topErrorCell;
    private TextInfoPrivacyCell nativeInfoCell;
    private TextSettingsCell scanDocumentCell;

    private boolean[] nonLatinNames = new boolean[3];
    private boolean allowNonLatinName = true;

    private boolean documentOnly;

    private TextView plusTextView;

    private TextSettingsCell addDocumentCell;
    private TextSettingsCell deletePassportCell;
    private ShadowSectionCell addDocumentSectionCell;
    private LinearLayout emptyLayout;
    private ImageView emptyImageView;
    private TextView emptyTextView1;
    private TextView emptyTextView2;
    private TextView emptyTextView3;

    private EmptyTextProgressView emptyView;
    private TextInfoPrivacyCell passwordRequestTextView;
    private TextInfoPrivacyCell passwordInfoRequestTextView;
    private ImageView noPasswordImageView;
    private TextView noPasswordTextView;
    private TextView noPasswordSetTextView;
    private FrameLayout passwordAvatarContainer;
    private TextView passwordForgotButton;
    private int usingSavedPassword;
    private byte[] savedPasswordHash;
    private byte[] savedSaltedPassword;

    private String currentPicturePath;
    private ChatAttachAlert chatAttachAlert;
    private int uploadingFileType;

    private int emailCodeLength;

    private ArrayList<String> countriesArray = new ArrayList<>();
    private HashMap<String, String> countriesMap = new HashMap<>();
    private HashMap<String, String> codesMap = new HashMap<>();
    private HashMap<String, String> phoneFormatMap = new HashMap<>();
    private HashMap<String, String> languageMap;

    private boolean ignoreOnTextChange;
    private boolean ignoreOnPhoneChange;

    private static final int info_item = 1;
    private static final int done_button = 2;

    private final static int attach_photo = 0;
    private final static int attach_gallery = 1;
    private final static int attach_document = 4;

    private long secureSecretId;
    private byte[] secureSecret;
    private String currentEmail;
    private byte[] saltedPassword;

    private boolean ignoreOnFailure;
    private boolean callbackCalled;
    private PassportActivity presentAfterAnimation;

    private ArrayList<SecureDocument> documents = new ArrayList<>();
    private SecureDocument selfieDocument;
    private ArrayList<SecureDocument> translationDocuments = new ArrayList<>();
    private SecureDocument frontDocument;
    private SecureDocument reverseDocument;
    private HashMap<SecureDocument, SecureDocumentCell> documentsCells = new HashMap<>();
    private HashMap<String, SecureDocument> uploadingDocuments = new HashMap<>();
    private HashMap<TLRPC.TL_secureRequiredType, HashMap<String, String>> typesValues = new HashMap<>();
    private HashMap<TLRPC.TL_secureRequiredType, TextDetailSecureCell> typesViews = new HashMap<>();
    private HashMap<TLRPC.TL_secureRequiredType, TLRPC.TL_secureRequiredType> documentsToTypesLink = new HashMap<>();
    private HashMap<String, String> currentValues;
    private HashMap<String, String> currentDocumentValues;
    private HashMap<String, HashMap<String, String>> errorsMap = new HashMap<>();
    private HashMap<String, String> mainErrorsMap = new HashMap<>();
    private HashMap<String, String> fieldsErrors;
    private HashMap<String, String> documentsErrors;
    private HashMap<String, String> errorsValues = new HashMap<>();
    private CharSequence noAllDocumentsErrorText;
    private CharSequence noAllTranslationErrorText;

    private PassportActivityDelegate delegate;

    private boolean needActivityResult;

    private interface PassportActivityDelegate {
        void saveValue(TLRPC.TL_secureRequiredType type, String text, String json, TLRPC.TL_secureRequiredType documentType, String documentsJson, ArrayList<SecureDocument> documents, SecureDocument selfie, ArrayList<SecureDocument> translationDocuments, SecureDocument front, SecureDocument reverse, Runnable finishRunnable, ErrorRunnable errorRunnable);
        void deleteValue(TLRPC.TL_secureRequiredType type, TLRPC.TL_secureRequiredType documentType, ArrayList<TLRPC.TL_secureRequiredType> documentRequiredTypes, boolean deleteType, Runnable finishRunnable, ErrorRunnable errorRunnable);
        SecureDocument saveFile(TLRPC.TL_secureFile secureFile);
    }

    private interface ErrorRunnable {
        void onError(String error, String text);
    }

    private PhotoViewer.PhotoViewerProvider provider = new PhotoViewer.EmptyPhotoViewerProvider() {

        @Override
        public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index) {
            if (index < 0 || index >= currentPhotoViewerLayout.getChildCount()) {
                return null;
            }
            SecureDocumentCell cell = (SecureDocumentCell) currentPhotoViewerLayout.getChildAt(index);
            int coords[] = new int[2];
            cell.imageView.getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? 0 : AndroidUtilities.statusBarHeight);
            object.parentV
