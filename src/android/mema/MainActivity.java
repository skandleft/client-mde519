package android.mema;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.io.IOException;
import android.mema.R;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
        private static String TAG = "MainActivity";
        /** Called when the activity is first created. */
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main);
            TextView txt=(TextView)this.findViewById(R.id.txt);
            Button reg=(Button)this.findViewById(R.id.register);
            Button collect=(Button)this.findViewById(R.id.createLog);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean regComplete = prefs.getBoolean("register", false);
            if(regComplete) {
                txt.setText("Registration complete!\nWelcome "+prefs.getString("username", ""));
                reg.setVisibility(View.INVISIBLE);
            } else {
                collect.setVisibility(View.INVISIBLE);
            }


            final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
            Log.i(TAG, "Called " + ste[2].getMethodName()+ " of "+this.getClass().getName());

        }
        @Override
    	protected void onDestroy() {
    		super.onDestroy();
    		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
    		Log.i(TAG, "Called " + ste[2].getMethodName()+ " of "+this.getClass().getName());
                stopService(new Intent(this, DataSendingService.class));
    	}

 
        
    	@Override
    	protected void onPause() {
    		// TODO Auto-generated method stub
    		super.onPause();
    		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
    		Log.i(TAG, "Called " + ste[2].getMethodName()+ " of "+this.getClass().getName());
    	}

    	@Override
    	protected void onResume() {
    		// TODO Auto-generated method stub
    		super.onResume();
                TextView tv = (TextView) this.findViewById(R.id.register);
                if(tv.getVisibility()==View.INVISIBLE)
                {
                    TextView tv2 = (TextView) this.findViewById(R.id.createLog);
                    tv2.setVisibility(View.VISIBLE);
                }
    		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
    		Log.i(TAG, "Called " + ste[2].getMethodName()+ " of "+this.getClass().getName());
    	}

    	@Override
    	protected void onStart() {
    		// TODO Auto-generated method stub
    		super.onStart();
    		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
    		Log.i(TAG, "Called " + ste[2].getMethodName()+ " of "+this.getClass().getName());
    	}

    	@Override
    	protected void onStop() {
    		// TODO Auto-generated method stub
    		super.onStop();
    		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
    		Log.i(TAG, "Called " + ste[2].getMethodName()+ " of "+this.getClass().getName());
        }

        public void createLog(View view) throws IOException{
            TextView tv = (TextView) this.findViewById(R.id.createLog);
            tv.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Starting the collection...", Toast.LENGTH_SHORT).show();
    	    Intent ourIntent = new Intent(this,DataCollection.class);
    	    ourIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	    startActivity(ourIntent);
                 

        }
        
        public void register(View view) throws IOException{

    	    Intent ourIntent = new Intent(this,Register.class);
    	    ourIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	    startActivity(ourIntent);
                 
        }
        
}
