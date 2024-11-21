package com.example.cmput301f24mikasa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class AdminManageEventsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AdminEventAdapter adapter;
    private List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_events_list);

        eventList = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.events_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminEventAdapter(this, eventList);
        recyclerView.setAdapter(adapter);

        loadEvents(db);
    }

    private void loadEvents(FirebaseFirestore db) {
        db.collection("event")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            event.setEventID(document.getId());
                            eventList.add(event);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("AdminManageEvents", "Error fetching events: ", task.getException());
                        Toast.makeText(this, "Failed to fetch events.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String eventId = data.getStringExtra("eventId");
            if (eventId != null) {
                for (Event event : eventList) {
                    if (event.getEventID().equals(eventId)) {
                        event.setImageURL(null);
                        adapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    }
}




