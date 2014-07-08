/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package android.mema;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author lefteris
 */
public class Register extends Activity {
   
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.register);
        // ToDo add your GUI initialization code here        
    }
    
    public void register(View view)
    {
        TextView username,age;
        username =(TextView)this.findViewById(R.id.reg_username);
        age =(TextView)this.findViewById(R.id.reg_age);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        
        if(!username.toString().equals("") && !age.toString().equals(""))
        {
            editor.putString("username", username.getText().toString());
            editor.putString("age", age.getText().toString());
            editor.putBoolean("register", true);
        }

        editor.commit();
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://donald.di.uoa.gr:8580/mde519Server/master/register");
        try
        {
            List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(1);
            nameValuePairs.add(new BasicNameValuePair("id",username.getText().toString()));
            nameValuePairs.add(new BasicNameValuePair("age",age.getText().toString()));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response;
            response = client.execute(post);
            String result = EntityUtils.toString(response.getEntity());
            Log.i("VVVVV", result);
        }
        catch (IOException ex) 
        {   
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }

        Intent ourIntent = new Intent(this,MainActivity.class);
    	ourIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	startActivity(ourIntent);
    }
}
