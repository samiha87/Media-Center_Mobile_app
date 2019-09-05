package com.example.media_center.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.media_center.BluetoothLeService;
import com.example.media_center.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private int projState = 0;
    private int audioState = 0;
    private int audioMute = 0;
    private int audioVol = 0;
    private int lightsState = 0;
    private PageViewModel pageViewModel;
    public  View mRoot = null;
    private OnListFragmentInteractionListener mListener;
    private final static String TAG = "PlaceholderFragment";

    private ImageButton projButton = null;
    private ImageButton audioButton = null;
    private ImageButton audioButtonUp = null;
    private ImageButton audioButtonDown = null;
    private ImageButton lightsButton = null;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }
    public PlaceholderFragment() {

         }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        mRoot = root;
        projButton = root.findViewById(R.id.Projector);
        audioButton = root.findViewById(R.id.Audio);
        audioButtonUp = root.findViewById(R.id.audioup);
        audioButtonDown = root.findViewById(R.id.audiodown);
        lightsButton = root.findViewById(R.id.Lights);

        final Handler handler = new Handler();

        final Runnable r = new Runnable() {
            public void run() {
                updateAudioState();
                updateLightsState();
                updateProjState();
                handler.postDelayed(this, 500);
            }
        };

        handler.postDelayed(r, 500);
        projButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (projState == 0) {
                    projState = 1;
                    projButton.setBackgroundResource(R.drawable.projectoron);
                    mListener.onButtonPressed("#Proj,Pwr,On*");
                } else {
                    projState = 0;
                    projButton.setBackgroundResource(R.drawable.projectoroff);
                    mListener.onButtonPressed("#Proj,Pwr,Off*");
                }
            }
        });

        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audioState == 0) {
                    audioState = 1;
                    audioButton.setBackgroundResource(R.drawable.musicon);
                    if(projState == 1) mListener.onButtonPressed("#Audio,Vol,Mute,On*");
                    else mListener.onButtonPressed("#Audio,Pwr,On*");
                } else {
                    audioState = 0;
                    audioButton.setBackgroundResource(R.drawable.musicoff);
                    if(projState == 1) mListener.onButtonPressed("#Audio,Vol,Mute,Off*");
                    else mListener.onButtonPressed("#Audio,Pwr,Off*");
                }
            }
        });

        audioButtonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioState == 0) return;
                mListener.onButtonPressed("#Audio,Vol,Up*");
                audioButtonUp.setMaxWidth(audioButton.getMaxWidth() + 10);
                audioButtonUp.setMaxHeight(audioButton.getMaxHeight() + 10);
                
                SystemClock.sleep(10);
                audioButtonUp.setMaxWidth(audioButton.getMaxWidth() - 10);
                audioButtonUp.setMaxHeight(audioButton.getMaxHeight() - 10);

            }
        });

        audioButtonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioState == 0) return;
                mListener.onButtonPressed("#Audio,Vol,Down*");
                audioButtonDown.setMaxWidth(audioButtonDown.getMaxWidth() + 10);
                audioButtonDown.setMaxHeight(audioButtonDown.getMaxHeight() + 10);
                SystemClock.sleep(10);
                audioButtonDown.setMaxWidth(audioButtonDown.getMaxWidth() - 10);
                audioButtonDown.setMaxHeight(audioButtonDown.getMaxHeight() - 10);
            }
        });

        lightsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(lightsState == 0) {
                    lightsState = 1;
                    lightsButton.setBackgroundResource(R.drawable.lightbulbon);
                    mListener.onButtonPressed("#Lights,Pwr,On*");
                } else {
                    lightsState = 0;
                    lightsButton.setBackgroundResource(R.drawable.lightbulboff);
                    mListener.onButtonPressed("#Lights,Pwr,Off*");
                }

            }
        });
        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void updateProjState() {

        if (projState == 1) {
            projButton.setBackgroundResource(R.drawable.projectoron);
        } else {
            projButton.setBackgroundResource(R.drawable.projectoroff);
        }
    }

    private void updateAudioState() {
        // If audio through projector
        if(projState == 0) {
           audioState = 0;
        } else if (projState == 1) {
            audioState = 1;
        }

        if (audioState == 1) {
            audioButton.setBackgroundResource(R.drawable.musicon);
            audioButtonUp.setBackgroundResource(R.drawable.arrowactiveup);
            audioButtonDown.setBackgroundResource(R.drawable.arrowactivedown);
        } else {
            audioButton.setBackgroundResource(R.drawable.musicoff);
            audioButtonUp.setBackgroundResource(R.drawable.arrowup);
            audioButtonDown.setBackgroundResource(R.drawable.arrowdown);
        }
        if(audioState == 0) return;
        if (audioMute == 1) {
            audioButton.setBackgroundResource(R.drawable.musicon);
        } else {
            audioButton.setBackgroundResource(R.drawable.musicoff);
        }
    }
    private void updateLightsState() {
        if (audioState == 1) {
            audioButton.setBackgroundResource(R.drawable.musicon);
        } else {
            audioButton.setBackgroundResource(R.drawable.musicoff);
        }
    }

    public void bleData(String data) {
        if(data.contains("Proj")) {
            Log.d(TAG, "bleData: Projector");
            if(data.contains("Pwr")) {
                data = data.replace("#r,Proj,Pwr,", "");
                if(data.charAt(0) == '1') {
                    Log.d(TAG, "bleData: Projector: on");
                    projState = 1;

                } else if(data.charAt(0) =='0' ) {
                    Log.d(TAG, "bleData: Projector: off");
                    projState = 0;
                    // If audio control from projector
                    audioState = 0;
                }
            }

            if(data.contains("Aud")) {
                data = data.replace("#r,Aud,", "");

                if(data.contains("Pwr")) {
                    data = data.replace("Pwr,", "");
                    if (data.charAt(0) == '1') {
                        Log.d(TAG, "bleData: Audio: on");
                        // In case audio from projector

                        audioState = 1;

                    } else if (data.charAt(0) == '0') {
                        Log.d(TAG, "bleData: Audio: off");
                        audioState = 0;
                    }
                }

                if(data.contains("Mute")) {
                    data = data.replace("Mute,", "");
                    if (data.charAt(0) == '1') {
                        Log.d(TAG, "bleData: Audio Mute: on");
                        // In case audio from projector
                        audioMute = 1;

                    } else if (data.charAt(0) == '0') {
                        Log.d(TAG, "bleData: Audio Mute: off");
                        audioMute = 0;
                    }
                }

                if(data.contains("Vol")) {
                    data = data.replace("Vol,", "");
                    data = data.replace("\r", "2");
                    audioVol = Integer.valueOf(data);
                }

            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
       void onButtonPressed(String msg);
    }

}