package com.example.cmput301f24mikasa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * EventResultList is an Activity that displays the selected and canceled entrants for a specific event.
 * This class enables event organizers to manage event entrants by:
 * - Viewing selected and canceled entrants
 * - Notifying selected entrants
 * - Picking new entrants from the waiting list
 * - Finalizing the list of entrants for an event
 *
 * The class interacts with Firebase Firestore to retrieve and update event information,
 * including the waiting list, selected entrants, and canceled entrants.
 */
public class EventResultList extends AppCompatActivity {
    ArrayList<UserProfile> selectedEntrants;
    ArrayList<UserProfile> canceledEntrants;
    ArrayList<String> toUpdateFireStoreList;
    private String eventTitle;
    // List for canceled entrants
    ListView selectedEntrantsListView;             // ListView for selected entrants
    ListView canceledEntrantsListView;
    // ListView for canceled entrants
    UserProfileArrayAdapter selectedEntrantsAdapter; // Adapter for selected entrants
    UserProfileArrayAdapter canceledEntrantsAdapter; // Adapter for canceled entrants
    Button notifyButton;
    private Button backButton;
    private Button pickNewUserButton;
    private Button createFinalButton;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference eventsRef = db.collection("event");
    private CollectionReference notificationRef = db.collection("notification");
    @Override

    /**
     * Initializes the EventResultList activity, setting up the layout, retrieving event details, and
     * populating the lists of selected and canceled entrants.
     *
     * @param savedInstanceState The saved state of the activity
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_result_list);
        Intent intent = getIntent();
        selectedEntrants=new ArrayList<>();
        canceledEntrants=new ArrayList<>();
        // Fetch the event ID from the intent
        String eventID = intent.getStringExtra("eventID");
        String eventTitle = intent.getStringExtra("eventTitle");

        TextView changeText = findViewById(R.id.headerTextView);
        changeText.setText("Result List For "+ eventTitle);



        if (eventID == null) {
            Toast.makeText(this, "Event ID not found!", Toast.LENGTH_SHORT).show();
        }

        fetchSelectedList(eventID);
        fetchCancelledList(eventID);


        //set the name of the result list
        selectedEntrantsListView = findViewById(R.id.selected_entrants_list_view);
        canceledEntrantsListView = findViewById(R.id.canceled_entrants_list_view);

        selectedEntrantsAdapter = new UserProfileArrayAdapter(this, selectedEntrants);
        canceledEntrantsAdapter = new UserProfileArrayAdapter(this, canceledEntrants);

        selectedEntrantsListView.setAdapter(selectedEntrantsAdapter);
        canceledEntrantsListView.setAdapter(canceledEntrantsAdapter);


        pickNewUserButton = findViewById(R.id.picker_button);
        //have to notify those waitinglist that they have not been chosen
        notifyButton = findViewById(R.id.notify_the_selected);
        //Cancelled users ie when they dont accept will automatically recieve
        //a you canceled notification when they say no
        backButton = findViewById(R.id.go_back);
        createFinalButton=findViewById(R.id.make_final_list);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the input field and show a toast message
                Intent intent = new Intent(EventResultList.this, WaitingListActivity.class);
                intent.putExtra("eventID", eventID);
                intent.putExtra("eventTitle", eventTitle);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
        //I can infinetly notify, that NEEDS TO BE FIXED!!!!!!!!!!!!!
        notifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the input field and show a toast message
                Intent intent = new Intent(EventResultList.this, OrganizerSendsNotification.class);
                intent.putExtra("eventID", eventID);
                intent.putExtra("eventTitle", eventTitle);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
        //I ALSO NEED TO NOTIFY THE USER THAT I PICKED THEM
        pickNewUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                eventsRef.document(eventID).get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> waitingList = (List<String>) documentSnapshot.get("waitingList");
                        Long chosenAmount = documentSnapshot.getLong("chosenAmount");

                        if (waitingList == null || waitingList.isEmpty()) {
                            Toast.makeText(EventResultList.this, "No users in the waiting list.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (chosenAmount == null) {
                            Toast.makeText(EventResultList.this, "Chosen amount not set for this event.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int selectedEntrantsSize = selectedEntrants.size();
                        Toast.makeText(EventResultList.this, "current size " + selectedEntrantsSize, Toast.LENGTH_SHORT).show();
                        Toast.makeText(EventResultList.this, "selected size  " + chosenAmount.intValue(), Toast.LENGTH_SHORT).show();

                        int remainingSelections = chosenAmount.intValue() - selectedEntrantsSize;
                        Toast.makeText(EventResultList.this, "choice left " + remainingSelections, Toast.LENGTH_SHORT).show();

                        if (remainingSelections <= 0) {
                            Toast.makeText(EventResultList.this, "No more selections allowed.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Filter out already selected device IDs from waiting list
                        List<String> selectableUsers = new ArrayList<>(waitingList);
                        for (UserProfile profile : selectedEntrants) {
                            selectableUsers.remove(profile.getName());
                        }

                        if (selectableUsers.isEmpty()) {
                            Toast.makeText(EventResultList.this, "No more users available to pick.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Randomly select a new device ID
                        String randomDeviceID = selectableUsers.get(new java.util.Random().nextInt(selectableUsers.size()));

                        // Push the selected device ID as a new element in Firestore's selectedEntrants list
                        eventsRef.document(eventID).update("selectedEntrants", FieldValue.arrayUnion(randomDeviceID))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(EventResultList.this, "New user selected successfully!", Toast.LENGTH_SHORT).show();

                                    // Now remove the selected user from the waitingList in Firestore
                                    eventsRef.document(eventID).update("waitingList", FieldValue.arrayRemove(randomDeviceID))
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(EventResultList.this, "User removed from waiting list.", Toast.LENGTH_SHORT).show();
                                                //sending notification
                                                sendNotificationToUser(randomDeviceID, eventID, eventTitle);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(EventResultList.this, "Failed to update waiting list.", Toast.LENGTH_SHORT).show();
                                                e.printStackTrace();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(EventResultList.this, "Failed to update selected entrants.", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                });
                        fetchSelectedList(eventID);

                    } else {
                        Toast.makeText(EventResultList.this, "Event not found!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(EventResultList.this, "Error fetching event details.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
            }
        });
        createFinalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // Reference to the event document by eventID
                DocumentReference eventRef = db.collection("event").document(eventID);

                // Query the notification collection for documents related to the specific event and responsive = "yes"
                db.collection("notification")
                        .whereEqualTo("eventID", eventID)  // Match the event ID
                        .whereEqualTo("responsive", "yes") // Check if the response is "yes"
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                List<String> deviceIDsToAdd = new ArrayList<>();

                                // Collect all deviceIDs from matching documents
                                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                    String deviceID = documentSnapshot.getString("deviceID");
                                    if (deviceID != null) {
                                        deviceIDsToAdd.add(deviceID);
                                    }
                                }

                                if (!deviceIDsToAdd.isEmpty()) {
                                    // Add all deviceIDs to the finalEntrants array in the event document
                                    eventRef.update("finalEntrants", FieldValue.arrayUnion(deviceIDsToAdd.toArray()))
                                            .addOnSuccessListener(aVoid -> {
                                                // Now, update the finalListCreated field to "1"
                                                eventRef.update("finalListCreated", "1")
                                                        .addOnSuccessListener(aVoid1 -> {
                                                            Toast.makeText(view.getContext(), "Users added to final entrants successfully and final list created!", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(view.getContext(), "Failed to update final list creation status.", Toast.LENGTH_SHORT).show();
                                                            e.printStackTrace();
                                                        });})
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(view.getContext(), "Failed to update final entrants.", Toast.LENGTH_SHORT).show();
                                                e.printStackTrace();
                                            });
                                    eventRef.update("selectedEntrants", new ArrayList<>());
                                    eventRef.update("waitingList", new ArrayList<>());
                                } else {
                                    Toast.makeText(view.getContext(), "No responsive users to add.", Toast.LENGTH_SHORT).show();
                                }
                                Intent intent = new Intent(EventResultList.this, EventFinalListActivity.class);
                                intent.putExtra("eventID", eventID);  // Pass the event ID to the new activity
                                intent.putExtra("eventTitle", eventTitle);
                                startActivity(intent);
                            } else {
                                Toast.makeText(view.getContext(), "No matching notifications found.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(view.getContext(), "Error querying notifications.", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        });
            }
        });



    }

    /**
     * Sends a notification to a specific user to inform them that they have been selected for the event.
     *
     * @param deviceID The device ID of the user to notify
     * @param eventID The ID of the event
     * @param eventTitle The title of the event
     */

    void sendNotificationToUser(String deviceID, String eventID, String eventTitle) {
        String eventText = "Congratulations! You were selected for the " + eventTitle + " because someone cancelled.";

        // Create a new document reference in the notification collection
        DocumentReference documentReference = notificationRef.document();
        String notificationID = documentReference.getId();

        // Prepare data for Firestore
        HashMap<String, Object> notificationData = new HashMap<>();
        notificationData.put("deviceID", deviceID);
        notificationData.put("eventID", eventID);
        notificationData.put("text", eventText);
        notificationData.put("responsive", "1");
        notificationData.put("notificationID", notificationID);

        // Add data to Firestore and show success/failure toasts
        documentReference.set(notificationData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EventResultList.this, "Notification sent successfully to selected entrant: " + deviceID, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EventResultList.this, "Failed to send notification to selected entrant: " + deviceID, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Fetches the list of selected entrants for the specified event from Firestore
     * and updates the ListView with the selected entrants.
     *
     * @param eventID The ID of the event
     */
    void fetchSelectedList(String eventID) {
        selectedEntrants.clear();
        // Query the event document by eventID to get the waitingList array
        eventsRef.document(eventID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the waitingList array from the document
                        List<String> selectedIDs = (List<String>) documentSnapshot.get("selectedEntrants");
                        if (selectedIDs != null && !selectedIDs.isEmpty()) {
                            // For each deviceID in the waiting list, add it as a placeholder user name
                            for (String deviceID : selectedIDs) {
                                // Create a UserProfile with deviceID as the name
                                UserProfile userProfile = new UserProfile();
                                userProfile.setName(deviceID); // Set deviceID as a placeholder for name
                                //later on, instead of displaying deviceID will want to display later
                                //name associated with deviceID
                                selectedEntrants.add(userProfile);
                            }

                            // Notify the adapter to update the ListView
                            selectedEntrantsAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "Waiting list is empty for this event.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching event details", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }


    /**
     * Fetches the list of canceled entrants for the specified event from Firestore
     * and updates the ListView with the canceled entrants.
     *
     * @param eventID The ID of the event
     */
    //refactor to pull from CANCELLED LIST AND ALSO UPDATE NOTIFICATIONS
    private void fetchCancelledList(String eventID) {
        // Get the specific event document reference using the eventID
        eventsRef.document(eventID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Retrieve the canceledEntrants list from the event document
                        List<String> canceledDeviceIDs = (List<String>) documentSnapshot.get("cancelledEntrants");

                        if (canceledDeviceIDs != null && !canceledDeviceIDs.isEmpty()) {
                            // For each canceled deviceID, create a UserProfile and add it to the canceledEntrants list
                            for (String deviceID : canceledDeviceIDs) {
                                // Create a UserProfile for the canceled entrant
                                UserProfile userProfile = new UserProfile();
                                userProfile.setName(deviceID); // Set the deviceID as a placeholder for the name

                                // Add the user profile to the canceledEntrants list
                                canceledEntrants.add(userProfile);
                            }

                            // Notify the adapter to update the canceled entrants ListView
                            canceledEntrantsAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "No canceled entrants for this event.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching canceled entrants.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }


}



//when picking a replacement, notify them as well
//change sp tjat you use a proper getter for device id not name lol
//toast message for cancelled usaer
//no way to cancel a user who has not responded, except for the fact
//that they are not put into the final list
//cancelled = amount of times I can press pick new user