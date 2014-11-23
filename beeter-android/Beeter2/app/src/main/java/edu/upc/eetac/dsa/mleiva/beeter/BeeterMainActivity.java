package edu.upc.eetac.dsa.mleiva.beeter;

import android.app.ListActivity;

/**
 * Created by Administrador on 17/11/2014.
 */



    import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;

import edu.upc.eetac.dsa.mleiva.beeter.api.AppException;
import edu.upc.eetac.dsa.mleiva.beeter.api.BeeterAPI;
import edu.upc.eetac.dsa.mleiva.beeter.api.Sting;
import edu.upc.eetac.dsa.mleiva.beeter.api.StingCollection;

public class BeeterMainActivity extends ListActivity {
    private ArrayList<Sting> stingsList;
    private StingAdapter adapter;


    private final static String TAG = BeeterMainActivity.class.toString();


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beeter_main);

        stingsList = new ArrayList<Sting>();
        adapter = new StingAdapter(this, stingsList);
        setListAdapter(adapter);

        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("alicia", "alicia"
                        .toCharArray());
            }
        });
        (new FetchStingsTask()).execute();

    }


    private class FetchStingsTask extends
            AsyncTask<Void, Void, StingCollection> {
        private ProgressDialog pd;

        @Override
        protected StingCollection doInBackground(Void... params) {
            StingCollection stings = null;
            try {
                stings = BeeterAPI.getInstance(BeeterMainActivity.this)
                        .getStings();
            } catch (AppException e) {
                e.printStackTrace();
            }
            return stings;
        }


        @Override
        protected void onPostExecute(StingCollection result) {
            addStings(result);
            if (pd != null) {
                pd.dismiss();
            }
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(BeeterMainActivity.this);
            pd.setTitle("Searching...");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }


        private void addStings(StingCollection stings) {
            stingsList.addAll(stings.getStings());
            adapter.notifyDataSetChanged();
        }


    }
}