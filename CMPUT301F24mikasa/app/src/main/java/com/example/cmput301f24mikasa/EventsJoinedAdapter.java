package com.example.cmput301f24mikasa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

/**
 * Custom ArrayAdapter to display a list of events the user has signed up for.
 * Each list item includes the event title and a "View" button to navigate to
 * a detailed view of the selected event.
 *
 * @version 1.0
 */
public class EventsJoinedAdapter extends ArrayAdapter<QueryDocumentSnapshot> {
    private final Context context;
    private final List<QueryDocumentSnapshot> events;

    /**
     * Constructs an EventsJoinedAdapter with the specified context and list of events.
     *
     * @param context the current context
     * @param events  the list of events the user has signed up for
     */
    public EventsJoinedAdapter(Context context, List<QueryDocumentSnapshot> events) {
        super(context, R.layout.activity_events_signed_up_item, events);
        this.context = context;
        this.events = events;
    }

    /**
     * Provides a view for an adapter item at a specified position in the list.
     * This method inflates the layout for each event item if not already inflated,
     * and populates it with data from the associated event document.
     *
     * @param position    the position of the item within the adapter's data set
     * @param convertView the old view to reuse, if possible
     * @param parent      the parent that this view will eventually be attached to
     * @return the View for the specified position
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("ViewHolder") View rowView = inflater.inflate(R.layout.activity_events_signed_up_item, parent, false);

        // Initialize UI elements
        ImageView eventImageView = rowView.findViewById(R.id.event_image);
        TextView eventTitleText = rowView.findViewById(R.id.event_title);
        Button viewButton = rowView.findViewById(R.id.view_button);
        Button unjoinButton = rowView.findViewById(R.id.unjoin_button);

        QueryDocumentSnapshot event = events.get(position);
        String eventId = event.getId();

        eventTitleText.setText(event.getString("title")); // Display the event title
        String imageURL = event.getString("imageURL");
        Glide.with(this.getContext()).load(imageURL).into(eventImageView);


        // Handle unjoin button click
        unjoinButton.setOnClickListener(v -> {
            // Get the unique device ID (using ANDROID_ID)
            String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

            if (deviceId != null && !deviceId.isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference eventRef = db.collection("event").document(eventId);

                // Remove the device ID from the waiting list in Firestore
                eventRef.update("waitingList", FieldValue.arrayRemove(deviceId))
                        .addOnSuccessListener(aVoid -> {
                            // Show success message and refresh the event list
                            Toast.makeText(context, "Unjoined successfully!", Toast.LENGTH_SHORT).show();
                            events.remove(position);
                            notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to unjoin", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // If device ID is null or empty, show an error message
                Toast.makeText(context, "Failed to get device ID", Toast.LENGTH_SHORT).show();
            }
        });

        // Set an OnClickListener for the "View" button
        viewButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewEventsJoined.class);

            // Pass the event details to the next activity
            intent.putExtra("title", event.getString("title"));
            intent.putExtra("startDate", event.getString("startDate"));
            intent.putExtra("desc", event.getString("description"));
            intent.putExtra("price", event.getString("price"));
            intent.putExtra("imageURL", event.getString("imageURL"));

            // Start the activity
            context.startActivity(intent);
        });

        return rowView;
    }
}
