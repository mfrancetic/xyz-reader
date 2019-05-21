package com.example.xyzreader.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    private static final String ARG_ITEM_ID = "item_id";
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private ObservableScrollView mScrollView;
    private ViewPager viewPager;
    private ImageView mPhotoView;
    private int position;
    private String transitionName;
    private final String transitionNameKey = "transitionName";
    private final String titleKey = "title";
    private final String authorKey = "author";
    private final String dateKey = "date";
    private String title;
    private String date;
    private String author;
    private final String bodyKey = "body";
    private String body;
    private static final String positionKey = "position";
    private final String urlKey = "url";
    private String url;
    private static final String photoKey = "photo";

    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(positionKey, position);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        } else if (savedInstanceState != null) {
            mItemId = savedInstanceState.getLong(ARG_ITEM_ID);
        }
        if (getArguments().containsKey(positionKey)) {
            position = getArguments().getInt(positionKey);
        }
        getResources().getBoolean(R.bool.detail_is_card);
        setHasOptionsMenu(true);
    }

    private ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        viewPager = mRootView.findViewById(R.id.pager);
        mRootView.findViewById(R.id.coordinator_layout_detail);

        prepareSharedElementTransition();

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 26) {
            postponeEnterTransition();
        }

        /* If the savedInstanceState exists, retrieve the values under their key names */
        if (savedInstanceState != null) {
            author = savedInstanceState.getString(authorKey);
            title = savedInstanceState.getString(titleKey);
            date = savedInstanceState.getString(dateKey);
            body = savedInstanceState.getString(bodyKey);
            position = savedInstanceState.getInt(positionKey);
            url = savedInstanceState.getString(urlKey);
            transitionName = savedInstanceState.getString(transitionNameKey);
        } else {
            transitionName = photoKey + position;
        }

        mPhotoView = mRootView.findViewById(R.id.photo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            /* Set the transitionName to the photoView */
            mPhotoView.setTransitionName(transitionName);
        }

        /* Find the toolbar, set it as the support action bar and set the navigationIcon */
        Toolbar toolbar = mRootView.findViewById(R.id.toolbar_detail);
        getActivityCast().setSupportActionBar(toolbar);

        /* Set the NavigationOnClickListener to the toolbar */
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivityCast().onBackPressed();
            }
        });

        /* Set the display home button and title of the supportActionBar */
        if (getActivityCast().getSupportActionBar() != null) {
            getActivityCast().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getActivityCast().getSupportActionBar().setHomeButtonEnabled(true);
            getActivityCast().getSupportActionBar().setTitle("");
        }
        /* Inflate the menu from the R.menu.main */
        toolbar.inflateMenu(R.menu.main);

        mScrollView = mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollView.getScrollY();
            }
        });

        mRootView.findViewById(R.id.photo_container);

        /* Set an OnClickListener to the share_fab button*/
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        bindViews();
        return mRootView;
    }

    private Date parsePublishedDate() {
        if (date == null) {
            date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
        }
        try {
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }
        /* Find the views using their ID's */
        TextView titleView = mRootView.findViewById(R.id.article_title);
        TextView bylineView = mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = mRootView.findViewById(R.id.article_body);

        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            if (title == null) {
                title = mCursor.getString(ArticleLoader.Query.TITLE);
            }
            titleView.setText(title);
            if (author == null) {
                author = mCursor.getString(ArticleLoader.Query.AUTHOR);
            }
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + author
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + author
                                + "</font>"));
            }
            if (body == null) {
                body = String.valueOf(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
            }
            bodyView.setText(body.replaceAll("(\r\n|\n)", "<br />"));

            if (url == null) {
                url = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            }

            /* Using the Picasso library, load the photoView */
            Picasso.get().load(url).into(mPhotoView, new Callback() {
                @Override
                public void onSuccess() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        scheduleStartPostponedTransition(mPhotoView);
                    }
                }

                @Override
                public void onError(Exception e) {
                }
            });
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /* Inflate the menu */
        inflater.inflate(R.menu.main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.home) {
            /* When clicking the home button in the menu, go back to the parent activity */
            AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
            appCompatActivity.onBackPressed();
            appCompatActivity.supportFinishAfterTransition();
            return true;
        } else if (id == R.id.refresh) {
            /* When clicking the refresh button in the menu, recreate the activity */
            AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
            appCompatActivity.recreate();
        }
        return super.onOptionsItemSelected(item);
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        /* Start the postponedEnterTransition on the sharedElement */
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            getActivity().startPostponedEnterTransition();
                        }
                        return true;
                    }
                });
    }

    /**
     * Prepare the shared element transition while using the EnterSharedElementCallback
     */
    private void prepareSharedElementTransition() {
        Transition transition = TransitionInflater.from(getActivity().getBaseContext()).inflateTransition(
                R.transition.image_shared_element_transition
        );
        setSharedElementEnterTransition(transition);
        setEnterSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                ArticleDetailFragment currentFragment = (ArticleDetailFragment) Objects.requireNonNull(viewPager.getAdapter())
                        .instantiateItem(viewPager, position);

                View view = currentFragment.getView();
                if (view == null) {
                    return;
                }
                sharedElements.put(names.get(0), view.findViewById(R.id.photo));
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(authorKey, author);
        savedInstanceState.putString(titleKey, title);
        savedInstanceState.putString(bodyKey, body);
        savedInstanceState.putString(dateKey, date);
        savedInstanceState.putInt(positionKey, position);
        savedInstanceState.putString(urlKey, url);
        savedInstanceState.putString(transitionNameKey, transitionName);
        savedInstanceState.putLong(ARG_ITEM_ID, mItemId);
        super.onSaveInstanceState(savedInstanceState);
    }
}