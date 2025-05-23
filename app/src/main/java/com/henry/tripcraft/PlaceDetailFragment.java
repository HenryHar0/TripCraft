package com.henry.tripcraft;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PlaceDetailFragment extends Fragment {

    private static final String ARG_PLACE_DATA = "place_data";
    private static final String ARG_API_KEY = "api_key";

    private PlaceData placeData;
    private String apiKey;
    private RecyclerView detailRecyclerView;
    private PlaceAdapter11 detailAdapter;
    private ImageView backButton;

    public static PlaceDetailFragment newInstance(PlaceData placeData, String apiKey) {
        PlaceDetailFragment fragment = new PlaceDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PLACE_DATA, placeData);
        args.putString(ARG_API_KEY, apiKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            placeData = (PlaceData) getArguments().getSerializable(ARG_PLACE_DATA);
            apiKey = getArguments().getString(ARG_API_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_place_detail, container, false);

        detailRecyclerView = view.findViewById(R.id.detailRecyclerView);
        backButton = view.findViewById(R.id.backButton);

        setupRecyclerView();
        setupBackButton();

        return view;
    }

    private void setupRecyclerView() {
        if (placeData != null) {
            List<PlaceData> singlePlaceList = new ArrayList<>();
            singlePlaceList.add(placeData);

            detailAdapter = new PlaceAdapter11(singlePlaceList, apiKey);
            detailRecyclerView.setAdapter(detailAdapter);
            detailRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            // Hide fragment container
            View fragmentContainer = requireActivity().findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                fragmentContainer.setVisibility(View.GONE);
            }

            // Show the All Places section again
            View activitiesLabel = requireActivity().findViewById(R.id.activitiesLabel);
            if (activitiesLabel != null && activitiesLabel.getParent() instanceof View) {
                View allPlacesSection = (View) activitiesLabel.getParent();
                allPlacesSection.setVisibility(View.VISIBLE);
            }

            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });
    }
}