package com.devlomi.commune.utils;

import com.devlomi.commune.utils.network.FireManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Devlomi on 31/10/2017.
 */

//this class will update Presence (online or last seen)
public class PresenceUtil {

    private CompositeDisposable disposables;
    private FireManager fireManager;

    public PresenceUtil() {
        disposables = new CompositeDisposable();
        onConnect();
        fireManager = new FireManager();

    }

    DatabaseReference connectedRef;
    DatabaseReference presenceRef;
    ValueEventListener connectedListener;

    private void onConnect() {
        presenceRef = FireConstants.presenceRef.child(FireManager.getUid());
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = dataSnapshot.getValue(Boolean.class);
                if (connected) {
                    disposables.add(fireManager.setOnlineStatus().subscribe());
                } else {
                    disposables.add(fireManager.setLastSeen().subscribe());
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };


        presenceRef.onDisconnect().setValue(ServerValue.TIMESTAMP);

    }

    public void onPause() {
        connectedRef.removeEventListener(connectedListener);
        disposables.add(fireManager.setLastSeen().subscribe());


    }

    public void onResume() {
        disposables.add(fireManager.setOnlineStatus().subscribe());
        connectedRef.addValueEventListener(connectedListener);
    }

    public void onDestroy() {
        disposables.dispose();
    }

}
