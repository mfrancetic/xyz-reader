package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.transition.Transition;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.Scene;
import android.transition.Slide;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private CoordinatorLayout coordinatorLayout;

    private RecyclerView mRecyclerView;

    private String photo = "photo";

    private String transitionName;

    private DynamicHeightNetworkImageView sharedView;

    private int id;

    private String transitionNameKey = "transitionName";

    private String titleKey = "title";

    private String title;

    private String authorKey = "author";

    private String dateKey = "date";

    private String date;

    private String author;

    private String bodyKey = "body";

    private String body;

    private String idKey = "id";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        coordinatorLayout = findViewById(R.id.coordinator_layout);

//        final View toolbarContainerView = findViewById(R.id.toolbar_container);
//
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        sharedView = findViewById(R.id.thumbnail);

        prepareTransitions();
        postponeEnterTransition();

        getLoaderManager().initLoader(0, null, this);


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        if (savedInstanceState == null) {
            refresh();
        } else {
            savedInstanceState.getInt(idKey, id);
            savedInstanceState.getString(transitionNameKey, transitionName);
            savedInstanceState.getString(authorKey, author);
            savedInstanceState.getString(titleKey, title);
            savedInstanceState.getString(dateKey, date);
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);

    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        startPostponedEnterTransition();

        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.setHasFixedSize(true);

        Snackbar snackbarRefresh = Snackbar.make(coordinatorLayout, getString(R.string.snackbar_text_update), Snackbar.LENGTH_LONG);
        snackbarRefresh.getView().setPadding(6, 0, 6, 6);
        snackbarRefresh.show();
//        snackbarRefresh.setAction(R.string.snackbar_undo, new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Snackbar snackbarUndo = Snackbar.make(coordinatorLayout, getString(R.string.snackbar_refresh_undone), Snackbar.LENGTH_LONG);
//                snackbarUndo.show();
//                onStop();
//            }
//        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


//                    Slide slide = new Slide();
//                    slide.setSlideEdge(Gravity.TOP);
//
//                    TransitionManager.beginDelayedTransition(parent, slide);
//                    view.setVisibility(View.INVISIBLE);

//                    TransitionManager.go(Scene.getSceneForLayout(parent,
//                            R.layout.activity_article_list, ArticleListActivity.this));

//                    Intent intent = new Intent(this, DetailsActivity.class);
//// Pass data object in the bundle and populate details activity.
//                    intent.putExtra(DetailsActivity.EXTRA_CONTACT, contact);
//                    ActivityOptionsCompat options = ActivityOptionsCompat.
//                            makeSceneTransitionAnimation(this, (View)ivProfile, "profile");
//                    startActivity(intent, options.toBundle());
//
//                    Bundle bundle = ActivityOptions.makeSceneTransitionAnimation
//                            (ArticleListActivity.this).toBundle();
//
//                    Intent intent = new Intent(Intent.ACTION_VIEW,
//                            ItemsContract.Items.buildItemUri(getItemId
//                                    (vh.getAdapterPosition())));
//
//                    intent.putExtra("photo", contact);
//                    ActivityOptionsCompat options = ActivityOptionsCompat.
//                            makeSceneTransitionAnimation(this, (View)photo, "profile");
//                    startActivity(intent, options.toBundle());
////
//                int id = vh.getAdapterPosition();
//
//                String transitionName = photo + id;
//
//                sharedView.setTransitionName(transitionName);

                    sharedView = vh.thumbnailView;

                    id = vh.getAdapterPosition();

                    transitionName = "photo" + id;

                    sharedView.setTransitionName(transitionName);

                    Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(
                            ArticleListActivity.this,
                            sharedView, transitionName).toBundle();
//                            ViewCompat.getTransitionName(sharedView)).toBundle();
//                            ViewCompat.getTransitionName(sharedView)).toBundle();

                        Intent intent = new Intent(Intent.ACTION_VIEW, ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                        intent.putExtra("bundle", bundle);
                        intent.putExtra(idKey, id);
                        intent.putExtra(transitionNameKey, transitionName);
                        startActivity(intent, bundle);


//                        startActivity(new Intent(Intent.ACTION_VIEW,
//                            ItemsContract.Items.buildItemUri(getItemId
//                                    (vh.getAdapterPosition()))), bundle);
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
//            if (title == null) {
                title = mCursor.getString(ArticleLoader.Query.TITLE);
//            }
            holder.titleView.setText(title);
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));


//            holder.thumbnailView = sharedView;

//            sharedView = holder.thumbnailView;

//            sharedView.setTransitionName(transitionName);


        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);

        postponeEnterTransition();
    }

    private void prepareTransitions() {
        getWindow().setExitTransition(TransitionInflater.from(this).inflateTransition(R.transition.grid_exit_transition));
        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(id);
                if (viewHolder == null || viewHolder.itemView == null) {
                    return;
                }

                sharedElements.put(names.get(0), viewHolder.itemView.findViewById(R.id.thumbnail));
            }
        });
    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle savedInstanceState) {
//        savedInstanceState.putString(authorKey, author);
//        savedInstanceState.putString(titleKey, title);
//        savedInstanceState.putString(dateKey, date);
//        savedInstanceState.putString(transitionNameKey, transitionName);
//        savedInstanceState.putInt(idKey, id);
//        super.onSaveInstanceState(savedInstanceState);
//    }
}
