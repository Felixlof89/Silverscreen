package se.k3.antonochisak.kd323bassignment5.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.k3.antonochisak.kd323bassignment5.R;
import se.k3.antonochisak.kd323bassignment5.adapters.PopularMoviesAdapter;
import se.k3.antonochisak.kd323bassignment5.api.RestClient;
import se.k3.antonochisak.kd323bassignment5.api.model.ApiResponse;
import se.k3.antonochisak.kd323bassignment5.models.movie.Movie;

import static se.k3.antonochisak.kd323bassignment5.helpers.StaticHelpers.FIREBASE_CHILD;
import static se.k3.antonochisak.kd323bassignment5.helpers.StaticHelpers.FIREBASE_URL;

/**
 * Created by isak on 2015-04-24.
 */

public class PopularMoviesFragment extends Fragment
        implements Callback<List<ApiResponse>>, GridView.OnItemClickListener {

    // Tag for logging,
    private static final String TAG = PopularMoviesFragment.class.getSimpleName();

    // List of movies
    ArrayList<Movie> mMovies;

    // This is pushed to mFireBase
    HashMap<String, Object> mMovieMap;

    RestClient mRestClient;
    Firebase mFireBase;
    Firebase mRef;

    String mCurrentClickedMovie = "";

    CountDownTimer mVoteTimer;
    boolean mIsVoteTimerRunning = false;

    PopularMoviesAdapter mAdapter;

    @InjectView(R.id.gridView)
    GridView mMoviesGrid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMovies = new ArrayList<>();
        mMovieMap = new HashMap<>();

        mRestClient = new RestClient();
        mFireBase = new Firebase(FIREBASE_URL);
        mRef = mFireBase.child(FIREBASE_CHILD);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_popular_movies, container, false);
        // Inject views
        ButterKnife.inject(this, view);

        // Create adapter
        mAdapter = new PopularMoviesAdapter(mMovies, getActivity().getLayoutInflater());
        mMoviesGrid.setAdapter(mAdapter);

        // listener= GridView.OnItemClickListener
        mMoviesGrid.setOnItemClickListener(this);
        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // listener = Callback<List<ApiResponse>>
        mRestClient.getApiService().getPopular("images", this);
        initVoteTimer();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (!mIsVoteTimerRunning) {
            voteOnMovie(i);
            mVoteTimer.start();
            mIsVoteTimerRunning = true;
        }
    }

    void initVoteTimer() {
        // So that there can only be one vote per every 3 seconds
        mVoteTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                mIsVoteTimerRunning = false;
            }
        };
    }

    void voteOnMovie(final int i) {
        Movie movie = mMovies.get(i);

        // Very important
        mCurrentClickedMovie = movie.getSlugline();

        mMovieMap.put("title", movie.getTitle());
        mMovieMap.put("year", movie.getYear());
        mMovieMap.put("slugline", movie.getSlugline());
        mMovieMap.put("poster", movie.getPoster());
        mMovieMap.put("fanart", movie.getFanArt());

        mRef.child(mCurrentClickedMovie).updateChildren(mMovieMap, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                Toast.makeText(getActivity(), "Gillade " + mMovies.get(i).getTitle(), Toast.LENGTH_SHORT).show();
                updateVotes();
            }
        });
    }

    void updateVotes() {
        mRef.child(mCurrentClickedMovie + "/votes").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue() == null) {
                    mutableData.setValue(1);
                } else {
                    mutableData.setValue((Long) mutableData.getValue() + 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                if (firebaseError != null) {
                    Log.d(TAG + " Error", firebaseError.getMessage());
                }
            }
        });
    }

    @Override
    public void success(List<ApiResponse> apiResponses, Response response) {
        for (ApiResponse r : apiResponses) {
            Movie movie = new Movie.Builder()
                    .title(r.title)
                    .slugLine(r.ids.getSlug())
                    .poster(r.image.getPoster().getMediumPoster())
                    .fanArt(r.image.getFanArt().getFullFanArt())
                    .year(r.year)
                    .build();

            mMovies.add(movie);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void failure(RetrofitError error) {
        error.printStackTrace();
    }
}