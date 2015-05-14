package com.example.wilson.pushnotification;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    private TextView regId;
    private EditText message;
    private Button send;

    private void Setup(){
        regId = (TextView)findViewById(R.id.regId);
        message = (EditText)findViewById(R.id.message);
        send = (Button)findViewById(R.id.send);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Setup();

        MagicLenGCM magicLenGCM = new MagicLenGCM(this);
        magicLenGCM.openGCM();
        regId.setText(magicLenGCM.getRegistrationId());
        Log.i("regId", magicLenGCM.getRegistrationId());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
