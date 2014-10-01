/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package android.mema;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class Login extends Activity {

    /**
     * Called when the activity is first created.
     */
    private Set <String> servers = new HashSet();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.login);
        // ToDo add your GUI initialization code here        
    }
    
    public void login(View view)
    {
        Button login=(Button)this.findViewById(R.id.btnLogin);

        final TextView username, password;
        username =(TextView)this.findViewById(R.id.log_username);
        password =(TextView)this.findViewById(R.id.log_password);
        login.setVisibility(View.INVISIBLE);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = prefs.edit();
        
        if(!username.getText().toString().equals("") && !password.getText().toString().equals(""))
        {
            try {
                String url="";
                boolean down=true;
                HttpClient client = null;
                int Try=1;
                while(down)
                {

                HttpGet get = null;
                if(Try==1)
                    url = "http://ultra.di.uoa.gr:9080/master/";
                else if(Try==2)
                    url = "http://donald.di.uoa.gr:9080/master/";
                get = new HttpGet(url);
                Try++;
                HttpParams httpParameters = new BasicHttpParams();
                int timeoutConnection = 2500;
                HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                client = new DefaultHttpClient(httpParameters);
                HttpResponse res = null;

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
                            if(!xpp.getAttributeValue(null, "url").contains("localhost"))
                                servers.add(xpp.getAttributeValue(null, "url")+"/");
                        }
                    } 
                    eventType = xpp.next();
                }
                editor.putStringSet("servers", servers);
                
                List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("id",username.getText().toString()));
                nameValuePairs.add(new BasicNameValuePair("password",password.getText().toString()));
                String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
                url += "login?"+paramString;
                Log.i("URL IS",url);
                get = new HttpGet(url);
                res = client.execute(get);
                if(res.getStatusLine().getStatusCode()==400)
                {
                    Toast.makeText(this, "User id does not exist...", Toast.LENGTH_LONG).show();
                    login.setVisibility(View.VISIBLE);

                }
                else if(res.getStatusLine().getStatusCode()==418)
                {
                    Toast.makeText(this, "Incorrect password...", Toast.LENGTH_LONG).show();
                    login.setVisibility(View.VISIBLE);
                }
                else
                {
                    editor.putString("username", username.getText().toString());
                    editor.putString("password", password.getText().toString());
                    editor.putBoolean("login", true);
                    editor.putString("current_server", "http://ultra.di.uoa.gr:9080/");

                    editor.commit();
                    Intent ourIntent = new Intent(this,MainActivity.class);
                    ourIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    ourIntent.putExtra("login", true);
                    startActivity(ourIntent);

                }
            }
            }catch (IOException ex) {
                Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
            } catch (XmlPullParserException ex) {
                Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        else
        {
            Toast.makeText(this, "Please fill all the fields to continue...", Toast.LENGTH_LONG).show();
            login.setVisibility(View.VISIBLE);
        }

    

        
    }
}

