package com.example.flicks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.flicks.models.Movie;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class MovieDetailsActivity extends AppCompatActivity {

    // constants
    // the base URL for the API
    public final static String API_BASE_URL = "https://api.themoviedb.org/3";
    // the parameter name for the API key
    public final static String API_KEY_PARAM = "api_key";
    // tag for logging from this activity
    public final static String TAG = "MovieTrailerActivity";

    // the movie to display
    Movie movie;

    // key
    String key;

    // the view objects
    TextView tvTitle;
    TextView tvOverview;
    RatingBar rbVoteAverage;
    TextView tvVote;
    TextView tvRelease;
    ImageView ivBackdropImage;

    // context for rendering
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        // resolve the view objects
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvOverview = (TextView) findViewById(R.id.tvOverview);
        rbVoteAverage = (RatingBar) findViewById(R.id.rbVoteAverage);
        tvVote = (TextView) findViewById(R.id.tvVote);
        tvRelease = (TextView) findViewById(R.id.tvRelease);
        ivBackdropImage = (ImageView) findViewById(R.id.ivBackdropImage);

        // unwrap the movie passed in via intent, using its simple name as a key
        movie = (Movie) Parcels.unwrap(getIntent().getParcelableExtra(Movie.class.getSimpleName()));
        Log.d("MovieDetailsActivity", String.format("Showing details for '%s'", movie.getTitle()));

        // set the title and overview
        tvTitle.setText(movie.getTitle());
        tvOverview.setText(movie.getOverview());
        tvVote.setText("(" + String.format(Integer.toString(movie.getVoteCount())) + ")");
        tvRelease.setText("Release Date: " + movie.getReleaseDate());

        // vote average is 0..10, convert to 0..5 by dividing by 2
        float voteAverage = movie.getVoteAverage().floatValue();
        rbVoteAverage.setRating(voteAverage > 0 ? voteAverage / 2.0f : voteAverage);

        // set the backdrop
        // get the correct placeholder and imageview for the backdrop
        int placeholderId = R.drawable.flicks_backdrop_placeholder;
        ImageView imageView = ivBackdropImage;

        // build url for poster image
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // load image using glide
        context = imageView.getContext();
        Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions()
                        .transform(new RoundedCornersTransformation(15, 0))
                        .placeholder(R.drawable.flicks_movie_placeholder)
                        .error(placeholderId))
                .into(imageView);
    }

    public void onClick(View v) {
        // create intent for the new activity
        Intent intent = new Intent(context, MovieTrailerActivity.class);
        // pass the video id as a string extra
        String videoId = getIntent().getStringExtra("videoId");
        intent.putExtra("videoId", videoId);
        getVideos(videoId);
    }

    // get the videos that have been added to a movie
    private void getVideos(String videoId) {
        // create the url
        String url = API_BASE_URL + "/movie/" + videoId + "/videos";
        // set the request parameters
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key)); // API key, always required
        AsyncHttpClient client = new AsyncHttpClient();
        // execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    // load the results into movies list
                    JSONArray results = response.getJSONArray("results");
                    // if videos exist, get the first video's key
                    if (results.length() > 0) {
                        key = results.getJSONObject(0).getString("key");
                        // create intent for the new activity
                        Intent intent = new Intent(context, MovieTrailerActivity.class);
                        // pass the key as a string extra
                        intent.putExtra("key", key);
                        // show the activity if videoId exists
                        context.startActivity(intent);
                    }
                    Log.i(TAG, String.format("Loaded %s movies", results.length()));
                } catch (JSONException e) {
                    logError("Failed to parse videos", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from videos endpoint", throwable, true);
            }
        });
    }

    // handle errors, log and alert user
    private void logError(String message, Throwable error, boolean alertUser) {
        // always log the error
        Log.e(TAG, message, error);
        // alert the user to avoid silent errors
        if (alertUser) {
            // show a long toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
