package com.devlomi.commune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.devlomi.commune.R;
import com.devlomi.commune.adapters.NumbersForContactAdapter;
import com.devlomi.commune.model.ExpandableContact;
import com.devlomi.commune.utils.ContactUtils;
import com.devlomi.commune.utils.IntentUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thoughtbot.expandablecheckrecyclerview.models.MultiCheckExpandableGroup;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.ArrayList;
import java.util.List;

public class SelectContactNumbersActivity extends AppCompatActivity {

    private RecyclerView rvNumbersForContactSelector;
    private FloatingActionButton fabSendContactSelect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contact_numbers);


        rvNumbersForContactSelector = findViewById(R.id.rv_numbers_for_contact_selector);
        fabSendContactSelect = findViewById(R.id.fab_send_contact_select);


        if (!getIntent().hasExtra(IntentUtils.EXTRA_CONTACT_LIST))
            return;


        getSupportActionBar().setTitle(R.string.select_numbers);
        List<ExpandableContact> result = getIntent().getParcelableArrayListExtra(IntentUtils.EXTRA_CONTACT_LIST);

        final NumbersForContactAdapter adapter = new NumbersForContactAdapter(result);

        //EXPAND ALL GROUPS
        adapter.toggleAllGroups();

        setItemsChecked(adapter);

        rvNumbersForContactSelector.setLayoutManager(new LinearLayoutManager(this));
        rvNumbersForContactSelector.setAdapter(adapter);


        fabSendContactSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getting selected numbers from contacts
                List<ExpandableContact> contactNameList = ContactUtils.getContactsFromExpandableGroups((List<? extends ExpandableGroup<?>>) adapter.getGroups());
                Intent intent = new Intent();
                intent.putParcelableArrayListExtra(IntentUtils.EXTRA_CONTACT_LIST, (ArrayList<? extends Parcelable>) contactNameList);
                setResult(RESULT_OK, intent);
                finish();
            }
        });


    }


    //set all numbers as Checked
    private void setItemsChecked(NumbersForContactAdapter adapter) {
        for (int i = 0; i < adapter.getGroups().size(); i++) {
            MultiCheckExpandableGroup group = (MultiCheckExpandableGroup) adapter.getGroups().get(i);
            for (int x = 0; x < group.getItems().size(); x++) {
                group.checkChild(x);
            }
        }
    }
}
