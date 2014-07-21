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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class Register extends Activity {
   
    /**
     * Called when the activity is first created.
     */
    private Set <String> servers = new HashSet();
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.register);
        // ToDo add your GUI initialization code here        
    }
    
    public void register(View view)
    {
        Button register=(Button)this.findViewById(R.id.btnRegister);
        register.setVisibility(View.INVISIBLE);
        Toast.makeText(this, "Please wait while you are being registered in our servers", Toast.LENGTH_LONG).show();

        final TextView username,age;
        username =(TextView)this.findViewById(R.id.reg_username);
        age =(TextView)this.findViewById(R.id.reg_age);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = prefs.edit();
        
        if(!username.toString().equals("") && !age.toString().equals(""))
        {
            editor.putString("username", username.getText().toString());
            editor.putString("age", age.getText().toString());
            editor.putBoolean("register", true);
        }

        

        Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String current_server = "";
            boolean down=true;
            int Try=1;
            HttpClient client = null;
            while(down)
            {
                HttpGet get = null;
                if(Try==1)
                    get = new HttpGet("http://ultra.di.uoa.gr:8580/mde519Server/master/");
                else if(Try==2)
                    get = new HttpGet("http://donald.di.uoa.gr:8580/mde519Server/master/");
                Try++;
                HttpParams httpParameters = new BasicHttpParams();
                int timeoutConnection = 2500;
                HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                client = new DefaultHttpClient(httpParameters);

                HttpResponse res;

                try {
                    String result=null;
                    res = client.execute(get);
                    down=false;
                    String xml=EntityUtils.toString(res.getEntity());
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(new StringReader (xml));
                    int eventType = xpp.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if(eventType == XmlPullParser.START_TAG) {
                            if(xpp.getAttributeCount()==2)
                            {
                                servers.add(xpp.getAttributeValue(null, "url"));
                            }
                        } 
                        eventType = xpp.next();
                    }
                    editor.putStringSet("servers", servers);
                    if(Try==2)
                    {
                        current_server="http://ultra.di.uoa.gr:8580/mde519Server/";
                        editor.putString("current_server", "http://ultra.di.uoa.gr:8580/mde519Server/");
                    }
                    else if(Try==3)
                    {
                        current_server="http://donald.di.uoa.gr:8580/mde519Server/";
                        editor.putString("current_server", "http://donald.di.uoa.gr:8580/mde519Server/");
                    }

                } catch (IOException ex) {
                    Logger.getLogger(Register.class.getName()).log(Level.SEVERE, null, ex);
                } catch (XmlPullParserException ex) {
                    Logger.getLogger(Register.class.getName()).log(Level.SEVERE, null, ex);
                }    
            }
            HttpPost post;
            for(String server : servers)
            {
                post = new HttpPost(server+"master/register");
                try
                {
                    List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(1);
                    nameValuePairs.add(new BasicNameValuePair("id",username.getText().toString()));
                    nameValuePairs.add(new BasicNameValuePair("age",age.getText().toString()));
                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    HttpResponse response;
                    response = client.execute(post);
                    String result = EntityUtils.toString(response.getEntity());
                }
                catch (IOException ex) 
                {   
                    Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            servers.remove(current_server);
            editor.putStringSet("servers", servers);
            editor.commit();

            Intent ourIntent = new Intent(Register.this,MainActivity.class);
            ourIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ourIntent.putExtra("register", true);
            startActivity(ourIntent);
          }
        };

        new Thread(runnable).start();
        //editor.commit();
        
    }
}
